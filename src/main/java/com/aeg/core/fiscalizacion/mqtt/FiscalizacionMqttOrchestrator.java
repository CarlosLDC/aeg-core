package com.aeg.core.fiscalizacion.mqtt;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.FiscalMqttTopics;
import com.aeg.core.fiscalizacion.mqtt.activity.FiscalizacionActivityResult;
import com.aeg.core.fiscalizacion.mqtt.activity.FiscalizacionActivityStore;
import com.aeg.core.fiscalizacion.mqtt.dto.PtrFiscalizarMessage;
import com.aeg.core.fiscalizacion.mqtt.sse.FiscalizacionSseNotifier;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.printer.Printer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FiscalizacionMqttOrchestrator {

    private final FiscalizacionMqttSettings settings;
    private final FiscalizacionPreconditionValidator preconditionValidator;
    private final FiscalizacionPayloadBuilder payloadBuilder;
    private final FiscalizacionCompletionService completionService;
    private final FiscalizacionSessionRegistry sessionRegistry;
    private final FiscalizacionActivityStore activityStore;
    private final FiscalizacionSseNotifier sseNotifier;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;

    public FiscalizacionMqttOrchestrator(
            FiscalizacionMqttSettings settings,
            FiscalizacionPreconditionValidator preconditionValidator,
            FiscalizacionPayloadBuilder payloadBuilder,
            FiscalizacionCompletionService completionService,
            FiscalizacionSessionRegistry sessionRegistry,
            FiscalizacionActivityStore activityStore,
            FiscalizacionSseNotifier sseNotifier,
            MqttService mqttService,
            @Qualifier("mqttObjectMapper") ObjectMapper objectMapper,
            @Qualifier("fiscalizacionTaskScheduler") TaskScheduler taskScheduler) {
        this.settings = settings;
        this.preconditionValidator = preconditionValidator;
        this.payloadBuilder = payloadBuilder;
        this.completionService = completionService;
        this.sessionRegistry = sessionRegistry;
        this.activityStore = activityStore;
        this.sseNotifier = sseNotifier;
        this.mqttService = mqttService;
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
            if (isPtrFiscalizarPayload(payload)) {
                sessionRegistry.find(compactMac).ifPresent(this::abandonForRestart);
                handlePtrFiscalizar(topic, compactMac, payload);
                return;
            }
            sessionRegistry.find(compactMac).ifPresent(session -> handleDeviceResponse(session, topic, payload));
            return;
        }

        if (isPtrFiscalizarPayload(payload)) {
            handlePtrFiscalizar(topic, compactMac, payload);
        }
    }

    private void handlePtrFiscalizar(String topic, String compactMac, String payload) {
        if (!FiscalMqttTopics.isCmdServerTopic(topic)) {
            activityStore.recordInbound(
                    topic, payload, compactMac, null, null,
                    FiscalizacionActivityResult.IGNORED,
                    "ptrFiscalizar must arrive on CmdServer topic", null);
            return;
        }

        PtrFiscalizarMessage message;
        try {
            message = objectMapper.readValue(payload, PtrFiscalizarMessage.class);
        } catch (IOException ex) {
            log.debug("Ignoring non-fiscalizacion payload: {}", ex.getMessage());
            return;
        }
        if (!FiscalizacionConstants.CMD_PTR_FISCALIZAR.equals(message.cmd())) {
            return;
        }

        String ptrReg = message.ptrReg();
        activityStore.recordInbound(
                topic, payload, compactMac, null, ptrReg,
                FiscalizacionActivityResult.RECEIVED, "ptrFiscalizar received", null);

        try {
            FiscalizacionContext context = preconditionValidator.validate(message, compactMac);
            FiscalizacionSession session = new FiscalizacionSession(context.compactMac(), context);
            sessionRegistry.register(session);

            String ackPayload = payloadBuilder.buildAckSuccess();
            String comandoTopic = FiscalMqttTopics.comandoTopic(session.compactMac());
            mqttService.publish(comandoTopic, ackPayload);
            session.setState(FiscalizacionSessionState.ACK_SENT);
            session.setAwaitingResponse(true);
            session.setAwaitingSince(Instant.now());
            session.setAwaitingTimeoutSeconds(settings.resultTimeoutSeconds());
            scheduleTimeout(session);

            activityStore.recordOutbound(
                    comandoTopic, ackPayload, session,
                    FiscalizacionActivityResult.PUBLISHED, "Published ACK success");
            sseNotifier.notifySessionStarted(session, comandoTopic, ackPayload);
            log.info(
                    "Fiscalizacion ACK sent ptrReg={} mac={} seal={}",
                    context.ptrReg(), context.compactMac(), context.precintoNro());
        } catch (FiscalizacionProtocolException ex) {
            publishValidationError(compactMac, ptrReg, ex.getMessage());
            activityStore.recordInbound(
                    topic, payload, compactMac, null, ptrReg,
                    FiscalizacionActivityResult.REJECTED, ex.getMessage(), null);
            log.warn("Fiscalizacion rejected mac={} ptrReg={}: {}", compactMac, ptrReg, ex.getMessage());
        }
    }

    private void publishValidationError(String compactMac, String ptrReg, String msj) {
        String comandoTopic = FiscalMqttTopics.comandoTopic(compactMac);
        String ackPayload = payloadBuilder.buildAckError(msj);
        mqttService.publish(comandoTopic, ackPayload);
        activityStore.recordOutboundNoSession(
                comandoTopic, ackPayload, compactMac, ptrReg,
                FiscalizacionActivityResult.PUBLISHED, "Published ACK error: " + msj);
    }

    private void handleDeviceResponse(FiscalizacionSession session, String topic, String payload) {
        if (!FiscalMqttTopics.isRespuestaTopic(topic)) {
            activityStore.recordInbound(
                    topic, payload, session.compactMac(), session.printerId(), session.context().ptrReg(),
                    FiscalizacionActivityResult.IGNORED,
                    "Device responses must arrive on Respuesta topic", session.state());
            return;
        }
        synchronized (session) {
            if (!session.isAwaitingResponse()) {
                activityStore.recordInbound(
                        topic, payload, session.compactMac(), session.printerId(), session.context().ptrReg(),
                        FiscalizacionActivityResult.IGNORED,
                        "Session not awaiting response", session.state());
                return;
            }
            try {
                JsonNode root = objectMapper.readTree(payload);
                if (!root.isObject()) {
                    activityStore.recordInbound(
                            topic, payload, session.compactMac(), null, session.context().ptrReg(),
                            FiscalizacionActivityResult.IGNORED, "Expected object response", session.state());
                    return;
                }
                String cmd = root.path("cmd").asText(null);
                if (cmd != null) {
                    cmd = cmd.trim();
                }
                if (!FiscalizacionConstants.CMD_RX_PTR_FISCALIZAR_REMOTO.equalsIgnoreCase(cmd)) {
                    activityStore.recordInbound(
                            topic, payload, session.compactMac(), null, session.context().ptrReg(),
                            FiscalizacionActivityResult.IGNORED,
                            "Mismatched object response cmd=" + cmd, session.state());
                    log.warn(
                            "Fiscalizacion ignored device response mac={} state={} cmd={} payloadSnippet={}",
                            session.compactMac(), session.state(), cmd, snippet(payload));
                    return;
                }
                int code = root.path("code").asInt(-1);
                cancelTimeout(session);
                session.clearAwaiting();
                activityStore.recordInbound(
                        topic, payload, session.compactMac(), null, session.context().ptrReg(),
                        FiscalizacionActivityResult.PROCESSED,
                        "Device response accepted code=" + code, session.state());

                if (code != 0) {
                    failSession(session, "Fiscalización falló en impresora (code=" + code + ")");
                    return;
                }

                Printer printer = completionService.complete(session.context());
                session.setPrinterId(printer.getId());
                session.setState(FiscalizacionSessionState.COMPLETED);
                sseNotifier.notifyResultAccepted(session, topic, payload);
                sseNotifier.notifySessionCompleted(session);
                activityStore.recordSessionEvent(
                        session, FiscalizacionActivityResult.COMPLETED,
                        "Printer created id=" + printer.getId(), FiscalizacionSessionState.COMPLETED);
                log.info(
                        "Fiscalizacion completed printerId={} ptrReg={} mac={}",
                        printer.getId(), session.context().ptrReg(), session.compactMac());
                sessionRegistry.remove(session.compactMac());
            } catch (FiscalizacionProtocolException ex) {
                failSession(session, ex.getMessage());
            } catch (IOException ex) {
                activityStore.recordInbound(
                        topic, payload, session.compactMac(), null, session.context().ptrReg(),
                        FiscalizacionActivityResult.IGNORED, "Invalid JSON response", session.state());
            }
        }
    }

    private void scheduleTimeout(FiscalizacionSession session) {
        ScheduledFuture<?> task = taskScheduler.schedule(
                () -> onTimeout(session.compactMac()),
                Instant.now().plusSeconds(settings.resultTimeoutSeconds()));
        session.setTimeoutTask(task);
    }

    private void onTimeout(String compactMac) {
        sessionRegistry.find(compactMac).ifPresent(session -> {
            synchronized (session) {
                if (session.isTerminal() || !session.isAwaitingResponse()) {
                    return;
                }
                failSession(session, "Timeout waiting for response at step ACK_SENT");
            }
        });
    }

    private void cancelTimeout(FiscalizacionSession session) {
        ScheduledFuture<?> task = session.timeoutTask();
        if (task != null) {
            task.cancel(false);
            session.setTimeoutTask(null);
        }
    }

    private void failSession(FiscalizacionSession session, String reason) {
        cancelTimeout(session);
        FiscalizacionSessionState failedAt = session.state();
        long elapsedMs = Duration.between(session.startedAt(), Instant.now()).toMillis();
        session.setLastError(reason);
        session.setState(FiscalizacionSessionState.FAILED);
        session.clearAwaiting();
        log.warn(
                "Fiscalizacion failed ptrReg={} mac={} failedAt={} elapsedMs={}: {}",
                session.context().ptrReg(), session.compactMac(), failedAt, elapsedMs, reason);
        activityStore.recordSessionEvent(session, FiscalizacionActivityResult.FAILED, reason, failedAt);
        sseNotifier.notifySessionFailed(session, reason, failedAt);
        sessionRegistry.remove(session.compactMac());
    }

    private void abandonForRestart(FiscalizacionSession session) {
        synchronized (session) {
            cancelTimeout(session);
            sessionRegistry.remove(session.compactMac());
            log.info(
                    "Fiscalizacion session replaced by new ptrFiscalizar mac={} previousState={}",
                    session.compactMac(), session.state());
        }
    }

    private boolean isPtrFiscalizarPayload(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            return node.isObject()
                    && FiscalizacionConstants.CMD_PTR_FISCALIZAR.equals(node.path("cmd").asText(null));
        } catch (IOException ex) {
            return false;
        }
    }

    private static String snippet(String payload) {
        if (payload == null) {
            return "";
        }
        String trimmed = payload.strip();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200) + "...";
    }
}
