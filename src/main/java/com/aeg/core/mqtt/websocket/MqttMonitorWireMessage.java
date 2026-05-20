package com.aeg.core.mqtt.websocket;

import java.time.Instant;

import com.aeg.core.mqtt.MqttInboundMessage;

public record MqttMonitorWireMessage(
		String type,
		String topic,
		String payload,
		Instant receivedAt,
		Integer qos) {

	public static MqttMonitorWireMessage fromInbound(MqttInboundMessage message) {
		return new MqttMonitorWireMessage(
				"message",
				message.topic(),
				message.payload(),
				message.receivedAt(),
				message.qos());
	}

	public static MqttMonitorWireMessage subscriptionChanged(String topic) {
		return new MqttMonitorWireMessage("subscription", topic, null, null, null);
	}
}
