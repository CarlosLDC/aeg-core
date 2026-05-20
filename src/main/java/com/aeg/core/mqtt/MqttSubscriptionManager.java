package com.aeg.core.mqtt;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.mqtt.dto.MqttSubscriptionResponse;
import com.aeg.core.mqtt.websocket.MqttMonitorSessionRegistry;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MqttSubscriptionManager {

	private static final int SUBSCRIPTION_QOS = 1;

	private final AtomicReference<String> activeTopic;
	private final MqttPahoMessageDrivenChannelAdapter mqttInbound;
	private final MqttMessageHistory history;
	private final MqttMonitorSessionRegistry sessionRegistry;
	private final boolean inboundEnabled;

	public MqttSubscriptionManager(
			@Value("${app.mqtt.inbound.topic:aeg/telemetry/#}") String defaultTopic,
			@Value("${app.mqtt.inbound.enabled:true}") boolean inboundEnabled,
			@Autowired(required = false) MqttPahoMessageDrivenChannelAdapter mqttInbound,
			MqttMessageHistory history,
			MqttMonitorSessionRegistry sessionRegistry) {
		this.activeTopic = new AtomicReference<>(MqttTopicValidator.validateAndNormalize(defaultTopic));
		this.inboundEnabled = inboundEnabled;
		this.mqttInbound = mqttInbound;
		this.history = history;
		this.sessionRegistry = sessionRegistry;
	}

	public MqttSubscriptionResponse current() {
		return new MqttSubscriptionResponse(activeTopic.get(), inboundEnabled && mqttInbound != null);
	}

	public synchronized MqttSubscriptionResponse updateTopic(String requestedTopic) {
		String normalized = MqttTopicValidator.validateAndNormalize(requestedTopic);
		if (!inboundEnabled || mqttInbound == null) {
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"MQTT inbound subscription is disabled on this server");
		}
		String previous = activeTopic.get();
		if (normalized.equals(previous)) {
			return new MqttSubscriptionResponse(normalized, true);
		}
		if (previous != null) {
			mqttInbound.removeTopic(previous);
		}
		mqttInbound.addTopic(normalized, SUBSCRIPTION_QOS);
		activeTopic.set(normalized);
		history.clear();
		sessionRegistry.broadcastSubscriptionChanged(normalized);
		log.info("MQTT monitor subscription changed: {} -> {}", previous, normalized);
		return new MqttSubscriptionResponse(normalized, true);
	}
}
