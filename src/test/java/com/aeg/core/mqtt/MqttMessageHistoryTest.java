package com.aeg.core.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class MqttMessageHistoryTest {

	@Test
	void keepsOnlyConfiguredCapacity() {
		MqttMessageHistory history = new MqttMessageHistory(3);
		for (int i = 0; i < 5; i++) {
			history.add(sample("topic-" + i));
		}
		assertThat(history.size()).isEqualTo(3);
		assertThat(history.recent(10)).extracting(MqttInboundMessage::topic)
				.containsExactly("topic-4", "topic-3", "topic-2");
	}

	@Test
	void respectsLimitOnRecentQuery() {
		MqttMessageHistory history = new MqttMessageHistory(50);
		history.add(sample("one"));
		history.add(sample("two"));
		assertThat(history.recent(1)).hasSize(1);
		assertThat(history.recent(1).getFirst().topic()).isEqualTo("two");
	}

	private static MqttInboundMessage sample(String topic) {
		return new MqttInboundMessage(topic, "{}", Instant.parse("2025-01-01T00:00:00Z"), 0);
	}
}
