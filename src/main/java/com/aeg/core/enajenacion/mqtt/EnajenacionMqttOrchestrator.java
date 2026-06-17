package com.aeg.core.enajenacion.mqtt;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityRecorder;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityResult;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.enajenacion.mqtt.dto.PtrEnajenarMessage;
import com.aeg.core.enajenacion.mqtt.sse.EnajenacionSseNotifier;
import com.aeg.core.mqtt.MqttService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EnajenacionMqttOrchestrator {

    private final EnajenacionPreconditionValidator preconditionValidator;
    private final EnajenacionSessionRegistry sessionRegistry;
    private final EnajenacionPayloadBuilder payloadBuilder;
    private final FiscalResponseValidator responseValidator;
    private final EnajenacionCompletionService completionService;
    private final MqttService mqttService;
    private final EnajenacionMqttSettings settings;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;
    private final EnajenacionSseNotifier sseNotifier;
    private final EnajenacionActivityRecorder activityRecorder;

    public EnajenacionMqttOrchestrator(
            EnajenacionPreconditionValidator preconditionValidator,
            EnajenacionSessionRegistry sessionRegistry,
            EnajenacionPayloadBuilder payloadBuilder,
            FiscalResponseValidator responseValidator,
            EnajenacionCompletionService completionService,
            MqttService mqttService,
            EnajenacionMqttSettings settings,
            @Qualifier("mqttObjectMapper") ObjectMapper objectMapper,
            @Qualifier("enajenacionTaskScheduler") TaskScheduler taskScheduler,
            EnajenacionSseNotifier sseNotifier,
            EnajenacionActivityRecorder activityRecorder) {
        this.preconditionValidator = preconditionValidator;
        this.sessionRegistry = sessionRegistry;
        this.payloadBuilder = payloadBuilder;
        this.responseValidator = responseValidator;
        this.completionService = completionService;
        this.mqttService = mqttService;
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.taskScheduler = taskScheduler;
        this.sseNotifier = sseNotifier;
        this.activityRecorder = activityRecorder;
    }

    public void handleInbound(String topic, String payload) {
        handleInboundWithOutcome(topic, payload);
    }

    /**
     * @return resultado solo cuando el payload es {@code ptrEnajenar}; vacío para respuestas del firmware.
     */
    public Optional<EnajenacionStartOutcome> handleInboundWithOutcome(String topic, String payload) {
        if (!settings.enabled()) {
            return Optional.empty();
        }
        String compactMac = FiscalMqttTopics.extractCompactMac(topic).orElse(null);
        if (compactMac == null) {
            return Optional.empty();
        }

        if (sessionRegistry.hasActiveSession(compactMac)) {
            if (isPtrEnajenarPayload(payload)) {
                sessionRegistry.find(compactMac).ifPresent(this::abandonSessionForRestart);
                return Optional.of(handlePtrEnajenarRequest(topic, compactMac, payload));
            }
            EnajenacionSession session = sessionRegistry.find(compactMac).orElseThrow();
            if (session.isAwaitingResponse()) {
                handleDeviceResponse(session, topic, payload);
            } else {
                log.debug("Ignoring inbound for MAC {} while session state={}", compactMac, session.state());
                activityRecorder.recordInbound(
                        topic,
                        payload,
                        compactMac,
                        session.printerId(),
                        session.context().fiscalSerial(),
                        EnajenacionActivityResult.IGNORED,
                        "Session not awaiting response in state " + session.state(),
                        session.state());
            }
            return Optional.empty();
        }

        return Optional.of(handlePtrEnajenarRequest(topic, compactMac, payload));
    }

    private EnajenacionStartOutcome handlePtrEnajenarRequest(String topic, String compactMac, String payload) {
        PtrEnajenarMessage message;
        try {
            message = objectMapper.readValue(payload, PtrEnajenarMessage.class);
        } catch (IOException ex) {
            log.debug("Ignoring non-enajenacion payload on fiscal topic: {}", ex.getMessage());
            return EnajenacionStartOutcome.skipped();
        }
        if (!EnajenacionConstants.CMD_PTR_ENAJENAR.equals(message.cmd())) {
            return EnajenacionStartOutcome.skipped();
        }
        String ptrReg = message.ptrReg();
        String payloadMac = message.macAddr();
        activityRecorder.recordInbound(
                topic,
                payload,
                compactMac,
                null,
                ptrReg,
                EnajenacionActivityResult.RECEIVED,
                "ptrEnajenar received",
                null);
        if (ptrReg == null || ptrReg.isBlank() || payloadMac == null || payloadMac.isBlank()) {
            String reason = "Invalid ptrEnajenar payload: missing ptrReg or macAddr";
            log.warn("Invalid ptrEnajenar payload for MAC {}: missing ptrReg or macAddr", compactMac);
            activityRecorder.recordInbound(
                    topic,
                    payload,
                    compactMac,
                    null,
                    ptrReg,
                    EnajenacionActivityResult.REJECTED,
                    reason,
                    null);
            return EnajenacionStartOutcome.rejected(reason);
        }

        try {
            EnajenacionContext context = preconditionValidator.validateAndBuildContext(ptrReg, compactMac, payloadMac);
            Long printerId = preconditionValidator.resolvePrinterId(ptrReg);
            if (printerId == null) {
                throw new EnajenacionProtocolException("Printer id not resolved for ptrReg " + ptrReg);
            }
            EnajenacionSession session = new EnajenacionSession(compactMac, printerId, context);
            sessionRegistry.register(session);
            log.info(
                    "Enajenacion session started printerId={} ptrReg={} mac={}",
                    printerId,
                    ptrReg,
                    compactMac);
            PublishedMqttCommand dnfCommand = publishDnf(session);
            sseNotifier.notifySessionStarted(session, dnfCommand.topic(), dnfCommand.payload());
            return EnajenacionStartOutcome.started();
        } catch (EnajenacionAlreadyCompletedException ex) {
            log.info("Ignoring ptrEnajenar for already enajenada printer ptrReg={}", ptrReg);
            activityRecorder.recordInbound(
                    topic,
                    payload,
                    compactMac,
                    null,
                    ptrReg,
                    EnajenacionActivityResult.IGNORED,
                    ex.getMessage(),
                    null);
            return EnajenacionStartOutcome.alreadyCompleted();
        } catch (EnajenacionProtocolException ex) {
            log.warn("Enajenacion preconditions failed ptrReg={} mac={}: {}", ptrReg, compactMac, ex.getMessage());
            activityRecorder.recordInbound(
                    topic,
                    payload,
                    compactMac,
                    null,
                    ptrReg,
                    EnajenacionActivityResult.REJECTED,
                    ex.getMessage(),
                    null);
            return EnajenacionStartOutcome.rejected(ex.getMessage());
        }
    }

    private void handleDeviceResponse(EnajenacionSession session, String topic, String payload) {
        synchronized (session) {
            if (!session.isAwaitingResponse()) {
                return;
            }
            boolean arrayPayload = isJsonArrayPayload(payload);
            if (session.awaitingKind() == EnajenacionAwaitingKind.ARRAY && !arrayPayload) {
                log.debug(
                        "Ignoring non-array device response mac={} state={}",
                        session.compactMac(),
                        session.state());
                recordIgnoredDeviceResponse(session, topic, payload, "Expected array response");
                return;
            }
            if (session.awaitingKind() == EnajenacionAwaitingKind.OBJECT && arrayPayload) {
                log.debug(
                        "Ignoring array device response while awaiting object mac={} state={}",
                        session.compactMac(),
                        session.state());
                recordIgnoredDeviceResponse(session, topic, payload, "Expected object response");
                return;
            }
            try {
                if (session.awaitingKind() == EnajenacionAwaitingKind.ARRAY) {
                    List<FiscalMqttResponseItem> items = parseArrayResponse(payload);
                    if (shouldIgnoreArrayResponse(session, items)) {
                        log.debug(
                                "Ignoring mismatched or command-shaped array mac={} state={}",
                                session.compactMac(),
                                session.state());
                        recordIgnoredDeviceResponse(
                                session, topic, payload, "Mismatched or command-shaped array response");
                        return;
                    }
                    advanceAfterArrayResponse(session, items);
                } else {
                    FiscalMqttResponseItem item = parseObjectResponse(payload);
                    if (shouldIgnoreObjectResponse(session, item)) {
                        log.debug(
                                "Ignoring mismatched object response mac={} state={} cmd={}",
                                session.compactMac(),
                                session.state(),
                                item.cmd());
                        recordIgnoredDeviceResponse(
                                session,
                                topic,
                                payload,
                                "Mismatched object response cmd=" + item.cmd());
                        return;
                    }
                    advanceAfterObjectResponse(session, item);
                }
                cancelTimeout(session);
                activityRecorder.recordInbound(
                        topic,
                        payload,
                        session.compactMac(),
                        session.printerId(),
                        session.context().fiscalSerial(),
                        EnajenacionActivityResult.PROCESSED,
                        "Device response accepted",
                        session.state());
            } catch (RuntimeException ex) {
                failSession(session, ex.getMessage());
            }
        }
    }

    private void recordIgnoredDeviceResponse(
            EnajenacionSession session, String topic, String payload, String detail) {
        activityRecorder.recordInbound(
                topic,
                payload,
                session.compactMac(),
                session.printerId(),
                session.context().fiscalSerial(),
                EnajenacionActivityResult.IGNORED,
                detail,
                session.state());
    }

    private void abandonSessionForRestart(EnajenacionSession session) {
        synchronized (session) {
            cancelTimeout(session);
            sessionRegistry.remove(session.compactMac());
            log.info(
                    "Enajenacion session replaced by new ptrEnajenar mac={} previousState={}",
                    session.compactMac(),
                    session.state());
        }
    }

    private boolean isPtrEnajenarPayload(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            return node.isObject()
                    && EnajenacionConstants.CMD_PTR_ENAJENAR.equals(node.path("cmd").asText(null));
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean shouldIgnoreArrayResponse(
            EnajenacionSession session, List<FiscalMqttResponseItem> items) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        if (looksLikeComandoCommandArray(items)) {
            return true;
        }
        FiscalMqttResponseItem first = items.get(0);
        if (first == null || first.cmd() == null) {
            return true;
        }
        String firstCmd = FiscalResponseValidator.normalizeCmd(first.cmd());
        return switch (session.state()) {
            case DNF_SENT -> false;
            case INVOICE_SENT -> !"proF".equalsIgnoreCase(firstCmd);
            case CREDIT_NOTE_SENT -> !"nroFacNC".equalsIgnoreCase(firstCmd);
            default -> true;
        };
    }

    private static boolean shouldIgnoreObjectResponse(
            EnajenacionSession session, FiscalMqttResponseItem item) {
        if (item == null || item.cmd() == null) {
            return true;
        }
        if (item.code() == null && item.dataS() != null && !item.dataS().isBlank()) {
            return true;
        }
        String cmd = FiscalResponseValidator.normalizeCmd(item.cmd());
        return switch (session.state()) {
            case FISCAL_RIF_SENT -> !EnajenacionConstants.CMD_FISCAL_AEG.equalsIgnoreCase(cmd);
            case HEADER_SENT, CONFIG_SENT -> !EnajenacionConstants.CMD_W_FILE_SPIFF.equalsIgnoreCase(cmd);
            case REG_STATUS_SENT -> !EnajenacionConstants.CMD_STA_INF.equalsIgnoreCase(cmd);
            case REPORT_Z_SENT -> !EnajenacionConstants.CMD_GEN_IMP_REP_Z.equalsIgnoreCase(cmd);
            default -> true;
        };
    }

    private static boolean looksLikeComandoCommandArray(List<FiscalMqttResponseItem> items) {
        for (FiscalMqttResponseItem item : items) {
            if (item == null) {
                continue;
            }
            if (item.code() == null && item.dataS() != null && !item.dataS().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJsonArrayPayload(String payload) {
        if (payload == null) {
            return false;
        }
        String trimmed = payload.stripLeading();
        return trimmed.startsWith("[");
    }

    private void advanceAfterArrayResponse(EnajenacionSession session, List<FiscalMqttResponseItem> items) {
        switch (session.state()) {
            case DNF_SENT -> {
                EnajenacionSessionState acceptedFrom = session.state();
                responseValidator.validateDnfResponse(items);
                session.setState(EnajenacionSessionState.DNF_OK);
                session.clearAwaiting();
                PublishedMqttCommand next = publishFiscalRif(session);
                sseNotifier.notifyStepTransition(session, acceptedFrom, next.topic(), next.payload());
            }
            case INVOICE_SENT -> {
                EnajenacionSessionState acceptedFrom = session.state();
                responseValidator.validateInvoiceResponse(items);
                session.setState(EnajenacionSessionState.INVOICE_OK);
                session.clearAwaiting();
                PublishedMqttCommand next = publishCreditNote(session);
                sseNotifier.notifyStepTransition(session, acceptedFrom, next.topic(), next.payload());
            }
            case CREDIT_NOTE_SENT -> {
                EnajenacionSessionState acceptedFrom = session.state();
                responseValidator.validateCreditNoteResponse(items);
                session.setState(EnajenacionSessionState.CREDIT_NOTE_OK);
                session.clearAwaiting();
                PublishedMqttCommand next = publishReportZ(session);
                sseNotifier.notifyStepTransition(session, acceptedFrom, next.topic(), next.payload());
            }
            default -> throw new EnajenacionProtocolException("Unexpected array response for state " + session.state());
        }
    }

    private void advanceAfterObjectResponse(EnajenacionSession session, FiscalMqttResponseItem item) {
        switch (session.state()) {
            case FISCAL_RIF_SENT -> {
                EnajenacionSessionState acceptedFrom = session.state();
                responseValidator.validateObjectResponse(item, EnajenacionConstants.CMD_FISCAL_AEG);
                session.setState(EnajenacionSessionState.FISCAL_RIF_OK);
                session.clearAwaiting();
                PublishedMqttCommand next = publishHeader(session);
                sseNotifier.notifyStepTransition(session, acceptedFrom, next.topic(), next.payload());
            }
            case HEADER_SENT -> {
                EnajenacionSessionState acceptedFrom = session.state();
                responseValidator.validateObjectResponse(item, EnajenacionConstants.CMD_W_FILE_SPIFF);
                session.setState(EnajenacionSessionState.HEADER_OK);
                session.clearAwaiting();
                PublishedMqttCommand next = publishConfigSpiffs(session);
                sseNotifier.notifyStepTransition(session, acceptedFrom, next.topic(), next.payload());
            }
            case CONFIG_SENT -> {
                EnajenacionSessionState acceptedFrom = session.state();
                responseValidator.validateObjectResponse(item, EnajenacionConstants.CMD_W_FILE_SPIFF);
                session.setState(EnajenacionSessionState.CONFIG_OK);
                session.clearAwaiting();
                PublishedMqttCommand next = afterConfigOk(session);
                sseNotifier.notifyStepTransition(session, acceptedFrom, next.topic(), next.payload());
            }
            case REG_STATUS_SENT -> {
                EnajenacionSessionState acceptedFrom = session.state();
                responseValidator.validateStaInfResponse(item, session.context().fiscalSerial());
                session.setState(EnajenacionSessionState.REG_STATUS_OK);
                session.clearAwaiting();
                PublishedMqttCommand next = publishInvoice(session);
                sseNotifier.notifyStepTransition(session, acceptedFrom, next.topic(), next.payload());
            }
            case REPORT_Z_SENT -> {
                EnajenacionSessionState acceptedFrom = session.state();
                responseValidator.validateReportZResponse(item);
                sseNotifier.notifyReportZAccepted(session);
                completionService.markEnajenada(session.printerId());
                session.setState(EnajenacionSessionState.COMPLETED);
                session.clearAwaiting();
                log.info(
                        "Enajenacion completed printerId={} ptrReg={} mac={}",
                        session.printerId(),
                        session.context().fiscalSerial(),
                        session.compactMac());
                sseNotifier.notifySessionCompleted(session);
                activityRecorder.recordSessionEvent(
                        session,
                        EnajenacionActivityResult.COMPLETED,
                        "Enajenacion completed",
                        EnajenacionSessionState.COMPLETED);
                sessionRegistry.remove(session.compactMac());
            }
            default -> throw new EnajenacionProtocolException("Unexpected object response for state " + session.state());
        }
    }

    private PublishedMqttCommand afterConfigOk(EnajenacionSession session) {
        if (settings.skipRegistrationStatus()) {
            return publishInvoice(session);
        }
        return publishRegistrationStatus(session);
    }

    private PublishedMqttCommand publishDnf(EnajenacionSession session) {
        return publishAndAwait(session, EnajenacionSessionState.DNF_SENT, EnajenacionAwaitingKind.ARRAY,
                payloadBuilder.buildDnfAlertPayload(), settings.dnfTimeoutSeconds());
    }

    private PublishedMqttCommand publishFiscalRif(EnajenacionSession session) {
        return publishAndAwait(session, EnajenacionSessionState.FISCAL_RIF_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildFiscalRifPayload(session.context()), settings.fiscalRifTimeoutSeconds());
    }

    private PublishedMqttCommand publishHeader(EnajenacionSession session) {
        return publishAndAwait(session, EnajenacionSessionState.HEADER_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildHeaderPayload(session.context()), settings.configTimeoutSeconds());
    }

    private PublishedMqttCommand publishConfigSpiffs(EnajenacionSession session) {
        return publishAndAwait(session, EnajenacionSessionState.CONFIG_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildConfigSpiffsPayload(), settings.configTimeoutSeconds());
    }

    private PublishedMqttCommand publishRegistrationStatus(EnajenacionSession session) {
        return publishAndAwait(session, EnajenacionSessionState.REG_STATUS_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildRegistrationStatusPayload(), settings.regStatusTimeoutSeconds());
    }

    private PublishedMqttCommand publishInvoice(EnajenacionSession session) {
        return publishAndAwait(session, EnajenacionSessionState.INVOICE_SENT, EnajenacionAwaitingKind.ARRAY,
                payloadBuilder.buildInvoicePayload(), settings.invoiceTimeoutSeconds());
    }

    private PublishedMqttCommand publishCreditNote(EnajenacionSession session) {
        return publishAndAwait(session, EnajenacionSessionState.CREDIT_NOTE_SENT, EnajenacionAwaitingKind.ARRAY,
                payloadBuilder.buildCreditNotePayload(
                        session.context(), session.invoiceNumber(), session.invoiceDate()),
                settings.creditNoteTimeoutSeconds());
    }

    private PublishedMqttCommand publishReportZ(EnajenacionSession session) {
        return publishAndAwait(session, EnajenacionSessionState.REPORT_Z_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildReportZPayload(), settings.reportZTimeoutSeconds());
    }

    private PublishedMqttCommand publishAndAwait(
            EnajenacionSession session,
            EnajenacionSessionState sentState,
            EnajenacionAwaitingKind awaitingKind,
            String payload,
            int timeoutSeconds) {
        synchronized (session) {
            cancelTimeout(session);
            String topic = FiscalMqttTopics.comandoTopic(session.compactMac());
            mqttService.publish(topic, payload);
            session.setState(sentState);
            session.setAwaiting(awaitingKind);
            scheduleTimeout(session, timeoutSeconds, sentState.name());
            log.info("Enajenacion step {} published mac={} topic={}", sentState, session.compactMac(), topic);
            activityRecorder.recordOutbound(
                    topic,
                    payload,
                    session,
                    EnajenacionActivityResult.PUBLISHED,
                    "Published " + sentState.name());
            return new PublishedMqttCommand(topic, payload);
        }
    }

    private void scheduleTimeout(EnajenacionSession session, int timeoutSeconds, String step) {
        ScheduledFuture<?> task = taskScheduler.schedule(
                () -> onTimeout(session.compactMac(), step),
                java.time.Instant.now().plusSeconds(timeoutSeconds));
        session.setTimeoutTask(task);
    }

    private void onTimeout(String compactMac, String step) {
        sessionRegistry.find(compactMac).ifPresent(session -> {
            synchronized (session) {
                if (session.isTerminal() || !session.isAwaitingResponse()) {
                    return;
                }
                failSession(session, "Timeout waiting for response at step " + step);
            }
        });
    }

    private void cancelTimeout(EnajenacionSession session) {
        ScheduledFuture<?> task = session.timeoutTask();
        if (task != null) {
            task.cancel(false);
            session.setTimeoutTask(null);
        }
    }

    private void failSession(EnajenacionSession session, String reason) {
        cancelTimeout(session);
        EnajenacionSessionState failedAtState = session.state();
        session.setLastError(reason);
        session.setState(EnajenacionSessionState.FAILED);
        session.clearAwaiting();
        log.warn(
                "Enajenacion failed printerId={} ptrReg={} mac={}: {}",
                session.printerId(),
                session.context().fiscalSerial(),
                session.compactMac(),
                reason);
        activityRecorder.recordSessionEvent(
                session,
                EnajenacionActivityResult.FAILED,
                reason,
                failedAtState);
        sseNotifier.notifySessionFailed(session, reason);
        sessionRegistry.remove(session.compactMac());
    }

    private List<FiscalMqttResponseItem> parseArrayResponse(String payload) {
        try {
            FiscalMqttResponseItem[] items = objectMapper.readValue(payload, FiscalMqttResponseItem[].class);
            return Arrays.asList(items);
        } catch (IOException ex) {
            throw new EnajenacionProtocolException("Invalid array response: " + ex.getMessage());
        }
    }

    private FiscalMqttResponseItem parseObjectResponse(String payload) {
        try {
            return objectMapper.readValue(payload, FiscalMqttResponseItem.class);
        } catch (IOException ex) {
            throw new EnajenacionProtocolException("Invalid object response: " + ex.getMessage());
        }
    }
}
