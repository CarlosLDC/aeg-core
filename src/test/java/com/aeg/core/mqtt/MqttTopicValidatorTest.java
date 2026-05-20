package com.aeg.core.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MqttTopicValidatorTest {

	@Test
	void acceptsWildcardTopic() {
		assertThat(MqttTopicValidator.validateAndNormalize("  aeg/devices/#  "))
				.isEqualTo("aeg/devices/#");
	}

	@Test
	void rejectsBlankTopic() {
		assertThatThrownBy(() -> MqttTopicValidator.validateAndNormalize("   "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("blank");
	}

	@Test
	void rejectsSystemTopic() {
		assertThatThrownBy(() -> MqttTopicValidator.validateAndNormalize("$SYS/broker"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
