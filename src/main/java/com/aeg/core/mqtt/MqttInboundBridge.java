package com.aeg.core.mqtt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import com.aeg.core.fiscal.FiscalTicketLatin2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MqttInboundBridge {

	private final ApplicationEventPublisher eventPublisher;

	public void handle(Message<?> message) {
		String topic = stringHeader(message, MqttHeaders.RECEIVED_TOPIC);
		String payload = stringifyPayload(message.getPayload(), topic);
		Integer qos = integerHeader(message, MqttHeaders.RECEIVED_QOS);
		MqttInboundMessage inbound = new MqttInboundMessage(topic, payload, Instant.now(), qos);
		log.info("MQTT inbound topic={} payload={}", topic, payload);
		eventPublisher.publishEvent(new MqttInboundReceivedEvent(this, inbound));
	}

	private static String stringifyPayload(Object payload, String topic) {
		if (payload == null) {
			return "";
		}
		if (payload instanceof byte[] bytes) {
			if (FiscalTicketLatin2.isFiscalPrinterTopic(topic)) {
				return FiscalTicketLatin2.decodePayload(bytes);
			}
			return new String(bytes, StandardCharsets.UTF_8);
		}
		return payload.toString();
	}

	private static String stringHeader(Message<?> message, String header) {
		Object value = message.getHeaders().get(header);
		return value == null ? null : value.toString();
	}

	private static Integer integerHeader(Message<?> message, String header) {
		Object value = message.getHeaders().get(header);
		if (value instanceof Integer integer) {
			return integer;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		return null;
	}
}
