package com.aeg.core.mqtt;

import java.time.Instant;

public record MqttInboundMessage(
		String topic,
		String payload,
		Instant receivedAt,
		Integer qos) {
}
