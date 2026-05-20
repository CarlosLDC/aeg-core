package com.aeg.core.mqtt;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aeg.core.mqtt.dto.MqttMonitorStatusResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MqttMonitorStatusService {

	private final MqttMessageHistory history;
	private final MqttInboundConnectionTracker connectionTracker;
	private final MqttSubscriptionManager subscriptionManager;

	@Value("${app.mqtt.inbound.enabled:true}")
	private boolean inboundEnabled;

	@Value("${app.mqtt.broker-url:tcp://localhost:1883}")
	private String brokerUrl;

	public MqttMonitorStatusResponse status() {
		Instant lastAt = history.lastMessageAt();
		if (lastAt == null) {
			lastAt = connectionTracker.lastMessageAt();
		}
		boolean connected = inboundEnabled && connectionTracker.isSubscribed();
		return new MqttMonitorStatusResponse(
				inboundEnabled,
				subscriptionManager.current().topic(),
				brokerUrl,
				connected,
				lastAt,
				history.size());
	}
}
