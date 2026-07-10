package com.aeg.core.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
class MqttInboundBridgeTest {

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private MqttInboundBridge bridge;

	@Test
	void publishesNormalizedInboundEvent() {
		var message = MessageBuilder.withPayload("{\"temp\":21}")
				.setHeader(MqttHeaders.RECEIVED_TOPIC, "devices/1/telemetry")
				.setHeader(MqttHeaders.RECEIVED_QOS, 1)
				.build();

		bridge.handle(message);

		ArgumentCaptor<MqttInboundReceivedEvent> captor = ArgumentCaptor.forClass(MqttInboundReceivedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		MqttInboundMessage inbound = captor.getValue().message();
		assertThat(inbound.topic()).isEqualTo("devices/1/telemetry");
		assertThat(inbound.payload()).isEqualTo("{\"temp\":21}");
		assertThat(inbound.qos()).isEqualTo(1);
		assertThat(inbound.receivedAt()).isNotNull();
	}

	@Test
	void decodesByteArrayPayload() {
		var message = MessageBuilder.withPayload("hola".getBytes())
				.setHeader(MqttHeaders.RECEIVED_TOPIC, "aeg/test")
				.build();

		bridge.handle(message);

		ArgumentCaptor<MqttInboundReceivedEvent> captor = ArgumentCaptor.forClass(MqttInboundReceivedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().message().payload()).isEqualTo("hola");
	}

	@Test
	void decodesAsciiJsonOnFiscalTopicAsUtf8() {
		String json = """
				{"cmd":"StaInf","code":0,"dataS":{"EstatusSeniat":"EN LINEA","ConexionWifi":"AP_IoT"}}\
				""";
		var message = MessageBuilder.withPayload(json.getBytes(StandardCharsets.UTF_8))
				.setHeader(
						MqttHeaders.RECEIVED_TOPIC,
						"/206EF1884C68/AEG_Fiscal/Integracion/Respuesta")
				.build();

		bridge.handle(message);

		ArgumentCaptor<MqttInboundReceivedEvent> captor = ArgumentCaptor.forClass(MqttInboundReceivedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().message().payload()).contains("EstatusSeniat");
		assertThat(captor.getValue().message().payload()).contains("EN LINEA");
	}

	@Test
	void decodesFiscalTopicPayloadAsLatin2() {
		byte[] payload = new byte[] {
				'{', '"', 'c', 'm', 'd', '"', ':', '"', 'S', 't', 'a', 'I', 'n', 'f', '"', ',',
				'"', 'd', 'a', 't', 'a', 'S', '"', ':', '"', (byte) 0xed, (byte) 0xf1, '"', '}'
		};
		var message = MessageBuilder.withPayload(payload)
				.setHeader(
						MqttHeaders.RECEIVED_TOPIC,
						"/20:6E:F1:88:4C:68/AEG_Fiscal/Integracion/Respuesta")
				.build();

		bridge.handle(message);

		ArgumentCaptor<MqttInboundReceivedEvent> captor = ArgumentCaptor.forClass(MqttInboundReceivedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().message().payload()).contains("íñ");
	}
}
