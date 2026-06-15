package com.aeg.core.enajenacion.mqtt;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.enajenacion.mqtt.dto.PtrEnajenarMessage;
import com.aeg.core.mqtt.MqttService;
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

    public EnajenacionMqttOrchestrator(
            EnajenacionPreconditionValidator preconditionValidator,
            EnajenacionSessionRegistry sessionRegistry,
            EnajenacionPayloadBuilder payloadBuilder,
            FiscalResponseValidator responseValidator,
            EnajenacionCompletionService completionService,
            MqttService mqttService,
            EnajenacionMqttSettings settings,
            @Qualifier("mqttObjectMapper") ObjectMapper objectMapper,
            @Qualifier("enajenacionTaskScheduler") TaskScheduler taskScheduler) {
        this.preconditionValidator = preconditionValidator;
        this.sessionRegistry = sessionRegistry;
        this.payloadBuilder = payloadBuilder;
        this.responseValidator = responseValidator;
        this.completionService = completionService;
        this.mqttService = mqttService;
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.taskScheduler = taskScheduler;
    }

    public void handleInbound(String topic, String payload) {
        if (!settings.enabled()) {
            return;
        }
        String compactMac = FiscalMqttTopics.extractCompactMac(topic).orElse(null);
        if (compactMac == null) {
            return;
        }

        if (sessionRegistry.hasActiveSession(compactMac)) {
            EnajenacionSession session = sessionRegistry.find(compactMac).orElseThrow();
            if (session.isAwaitingResponse()) {
                handleDeviceResponse(session, payload);
            } else {
                log.debug("Ignoring inbound for MAC {} while session state={}", compactMac, session.state());
            }
            return;
        }

        handlePtrEnajenarRequest(compactMac, payload);
    }

    private void handlePtrEnajenarRequest(String compactMac, String payload) {
        PtrEnajenarMessage message;
        try {
            message = objectMapper.readValue(payload, PtrEnajenarMessage.class);
        } catch (IOException ex) {
            log.debug("Ignoring non-enajenacion payload on fiscal topic: {}", ex.getMessage());
            return;
        }
        if (!EnajenacionConstants.CMD_PTR_ENAJENAR.equals(message.cmd())) {
            return;
        }
        String ptrReg = message.ptrReg();
        String payloadMac = message.macAddr();
        if (ptrReg == null || ptrReg.isBlank() || payloadMac == null || payloadMac.isBlank()) {
            log.warn("Invalid ptrEnajenar payload for MAC {}: missing ptrReg or macAddr", compactMac);
            return;
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
            publishDnf(session);
        } catch (EnajenacionAlreadyCompletedException ex) {
            log.info("Ignoring ptrEnajenar for already enajenada printer ptrReg={}", ptrReg);
        } catch (EnajenacionProtocolException ex) {
            log.warn("Enajenacion preconditions failed ptrReg={} mac={}: {}", ptrReg, compactMac, ex.getMessage());
        }
    }

    private void handleDeviceResponse(EnajenacionSession session, String payload) {
        synchronized (session) {
            if (!session.isAwaitingResponse()) {
                return;
            }
            try {
                if (session.awaitingKind() == EnajenacionAwaitingKind.ARRAY) {
                    List<FiscalMqttResponseItem> items = parseArrayResponse(payload);
                    advanceAfterArrayResponse(session, items);
                } else {
                    FiscalMqttResponseItem item = parseObjectResponse(payload);
                    advanceAfterObjectResponse(session, item);
                }
                cancelTimeout(session);
            } catch (RuntimeException ex) {
                failSession(session, ex.getMessage());
            }
        }
    }

    private void advanceAfterArrayResponse(EnajenacionSession session, List<FiscalMqttResponseItem> items) {
        switch (session.state()) {
            case DNF_SENT -> {
                responseValidator.validateDnfResponse(items);
                session.setState(EnajenacionSessionState.DNF_OK);
                session.clearAwaiting();
                publishFiscalRif(session);
            }
            case INVOICE_SENT -> {
                responseValidator.validateInvoiceResponse(items);
                session.setState(EnajenacionSessionState.INVOICE_OK);
                session.clearAwaiting();
                publishCreditNote(session);
            }
            case CREDIT_NOTE_SENT -> {
                responseValidator.validateCreditNoteResponse(items);
                session.setState(EnajenacionSessionState.CREDIT_NOTE_OK);
                session.clearAwaiting();
                publishReportZ(session);
            }
            default -> throw new EnajenacionProtocolException("Unexpected array response for state " + session.state());
        }
    }

    private void advanceAfterObjectResponse(EnajenacionSession session, FiscalMqttResponseItem item) {
        switch (session.state()) {
            case FISCAL_RIF_SENT -> {
                responseValidator.validateObjectResponse(item, EnajenacionConstants.CMD_FISCAL_AEG);
                session.setState(EnajenacionSessionState.FISCAL_RIF_OK);
                session.clearAwaiting();
                publishHeader(session);
            }
            case HEADER_SENT -> {
                responseValidator.validateObjectResponse(item, EnajenacionConstants.CMD_W_FILE_SPIFF);
                session.setState(EnajenacionSessionState.HEADER_OK);
                session.clearAwaiting();
                publishConfigSpiffs(session);
            }
            case CONFIG_SENT -> {
                responseValidator.validateObjectResponse(item, EnajenacionConstants.CMD_W_FILE_SPIFF);
                session.setState(EnajenacionSessionState.CONFIG_OK);
                session.clearAwaiting();
                afterConfigOk(session);
            }
            case REG_STATUS_SENT -> {
                responseValidator.validateStaInfResponse(item, session.context().fiscalSerial());
                session.setState(EnajenacionSessionState.REG_STATUS_OK);
                session.clearAwaiting();
                publishInvoice(session);
            }
            case REPORT_Z_SENT -> {
                responseValidator.validateReportZResponse(item);
                completionService.markEnajenada(session.printerId());
                session.setState(EnajenacionSessionState.COMPLETED);
                session.clearAwaiting();
                log.info(
                        "Enajenacion completed printerId={} ptrReg={} mac={}",
                        session.printerId(),
                        session.context().fiscalSerial(),
                        session.compactMac());
                sessionRegistry.remove(session.compactMac());
            }
            default -> throw new EnajenacionProtocolException("Unexpected object response for state " + session.state());
        }
    }

    private void afterConfigOk(EnajenacionSession session) {
        if (settings.skipRegistrationStatus()) {
            publishInvoice(session);
            return;
        }
        publishRegistrationStatus(session);
    }

    private void publishDnf(EnajenacionSession session) {
        publishAndAwait(session, EnajenacionSessionState.DNF_SENT, EnajenacionAwaitingKind.ARRAY,
                payloadBuilder.buildDnfAlertPayload(), settings.dnfTimeoutSeconds());
    }

    private void publishFiscalRif(EnajenacionSession session) {
        publishAndAwait(session, EnajenacionSessionState.FISCAL_RIF_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildFiscalRifPayload(session.context()), settings.fiscalRifTimeoutSeconds());
    }

    private void publishHeader(EnajenacionSession session) {
        publishAndAwait(session, EnajenacionSessionState.HEADER_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildHeaderPayload(session.context()), settings.configTimeoutSeconds());
    }

    private void publishConfigSpiffs(EnajenacionSession session) {
        publishAndAwait(session, EnajenacionSessionState.CONFIG_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildConfigSpiffsPayload(), settings.configTimeoutSeconds());
    }

    private void publishRegistrationStatus(EnajenacionSession session) {
        publishAndAwait(session, EnajenacionSessionState.REG_STATUS_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildRegistrationStatusPayload(), settings.regStatusTimeoutSeconds());
    }

    private void publishInvoice(EnajenacionSession session) {
        publishAndAwait(session, EnajenacionSessionState.INVOICE_SENT, EnajenacionAwaitingKind.ARRAY,
                payloadBuilder.buildInvoicePayload(), settings.invoiceTimeoutSeconds());
    }

    private void publishCreditNote(EnajenacionSession session) {
        publishAndAwait(session, EnajenacionSessionState.CREDIT_NOTE_SENT, EnajenacionAwaitingKind.ARRAY,
                payloadBuilder.buildCreditNotePayload(
                        session.context(), session.invoiceNumber(), session.invoiceDate()),
                settings.creditNoteTimeoutSeconds());
    }

    private void publishReportZ(EnajenacionSession session) {
        publishAndAwait(session, EnajenacionSessionState.REPORT_Z_SENT, EnajenacionAwaitingKind.OBJECT,
                payloadBuilder.buildReportZPayload(), settings.reportZTimeoutSeconds());
    }

    private void publishAndAwait(
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
        session.setLastError(reason);
        session.setState(EnajenacionSessionState.FAILED);
        session.clearAwaiting();
        log.warn(
                "Enajenacion failed printerId={} ptrReg={} mac={}: {}",
                session.printerId(),
                session.context().fiscalSerial(),
                session.compactMac(),
                reason);
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
