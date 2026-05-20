package com.aeg.core.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;

import com.aeg.core.mqtt.dto.MqttSubscriptionResponse;
import com.aeg.core.mqtt.websocket.MqttMonitorSessionRegistry;

@ExtendWith(MockitoExtension.class)
class MqttSubscriptionManagerTest {

	@Mock
	private MqttPahoMessageDrivenChannelAdapter mqttInbound;

	@Mock
	private MqttMonitorSessionRegistry sessionRegistry;

	private MqttMessageHistory history;
	private MqttSubscriptionManager manager;

	@BeforeEach
	void setUp() {
		history = new MqttMessageHistory(10);
		history.add(new MqttInboundMessage("old/topic", "x", java.time.Instant.now(), 0));
		manager = new MqttSubscriptionManager(
				"old/topic",
				true,
				mqttInbound,
				history,
				sessionRegistry);
	}

	@Test
	void updateTopicReplacesSingleSubscriptionAndClearsHistory() {
		MqttSubscriptionResponse response = manager.updateTopic("devices/+/telemetry");

		assertThat(response.topic()).isEqualTo("devices/+/telemetry");
		assertThat(response.active()).isTrue();
		verify(mqttInbound).removeTopic("old/topic");
		verify(mqttInbound).addTopic("devices/+/telemetry", 1);
		verify(sessionRegistry).broadcastSubscriptionChanged("devices/+/telemetry");
		assertThat(history.size()).isZero();
	}

	@Test
	void updateTopicNoOpWhenUnchanged() {
		MqttSubscriptionResponse response = manager.updateTopic("old/topic");

		assertThat(response.topic()).isEqualTo("old/topic");
		verify(mqttInbound, never()).removeTopic("old/topic");
		verify(mqttInbound, never()).addTopic("old/topic", 1);
		verify(sessionRegistry, never()).broadcastSubscriptionChanged(org.mockito.ArgumentMatchers.anyString());
	}
}
