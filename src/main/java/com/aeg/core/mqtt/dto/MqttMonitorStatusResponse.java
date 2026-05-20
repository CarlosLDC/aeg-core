package com.aeg.core.mqtt.dto;

import java.time.Instant;

public record MqttMonitorStatusResponse(
		boolean inboundEnabled,
		String subscribedTopic,
		String brokerUrl,
		boolean connected,
		Instant lastMessageAt,
		int bufferedMessageCount) {
}
