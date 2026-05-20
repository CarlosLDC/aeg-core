package com.aeg.core.mqtt;

final class MqttTopicValidator {

	private static final int MAX_LENGTH = 256;

	private MqttTopicValidator() {
	}

	static String validateAndNormalize(String topic) {
		if (topic == null) {
			throw new IllegalArgumentException("topic is required");
		}
		String normalized = topic.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("topic must not be blank");
		}
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("topic must be at most " + MAX_LENGTH + " characters");
		}
		if (normalized.contains("\u0000")) {
			throw new IllegalArgumentException("topic contains invalid characters");
		}
		if (normalized.startsWith("$")) {
			throw new IllegalArgumentException("system topics ($SYS/...) are not allowed");
		}
		if (normalized.contains("//")) {
			throw new IllegalArgumentException("topic must not contain empty levels");
		}
		if (!normalized.matches("^[\\w./#+-]+$")) {
			throw new IllegalArgumentException(
					"topic may only contain letters, digits, and / # + - _ .");
		}
		return normalized;
	}
}
