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

	@Test
	void truncatesOversizedPayloadAndTracksSizeAcrossAddAndClear() {
		MqttMessageHistory history = new MqttMessageHistory(2);
		String oversized = "A".repeat(MqttMessageHistory.MAX_PAYLOAD_LENGTH + 500);

		history.add(new MqttInboundMessage("topic-1", oversized, Instant.parse("2025-01-01T00:00:00Z"), 1));
		history.add(sample("topic-2"));
		history.add(sample("topic-3"));

		assertThat(history.size()).isEqualTo(2);
		history.add(new MqttInboundMessage("topic-4", oversized, Instant.parse("2025-01-01T00:00:01Z"), 1));
		assertThat(history.size()).isEqualTo(2);
		assertThat(history.recent(1).getFirst().payload()).hasSize(MqttMessageHistory.MAX_PAYLOAD_LENGTH);

		history.clear();
		assertThat(history.size()).isZero();
		assertThat(history.recent(10)).isEmpty();
	}

	private static MqttInboundMessage sample(String topic) {
		return new MqttInboundMessage(topic, "{}", Instant.parse("2025-01-01T00:00:00Z"), 0);
	}
}
