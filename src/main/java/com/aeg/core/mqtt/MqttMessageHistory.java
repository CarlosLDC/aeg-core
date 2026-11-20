package com.aeg.core.mqtt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageHistory {

	static final int MAX_PAYLOAD_LENGTH = 4_096;

	private final int capacity;
	private final Deque<MqttInboundMessage> messages = new ConcurrentLinkedDeque<>();
	private final AtomicInteger count = new AtomicInteger();
	private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>();

	public MqttMessageHistory(@Value("${app.mqtt.monitor.buffer-size:200}") int capacity) {
		this.capacity = Math.max(1, capacity);
	}

	public synchronized void add(MqttInboundMessage message) {
		MqttInboundMessage stored = truncatePayload(message);
		lastMessageAt.set(stored.receivedAt());
		messages.addFirst(stored);
		count.incrementAndGet();
		while (count.get() > capacity) {
			if (messages.pollLast() != null) {
				count.decrementAndGet();
			} else {
				count.set(0);
				break;
			}
		}
	}

	public List<MqttInboundMessage> recent(int limit) {
		int effectiveLimit = Math.min(Math.max(1, limit), capacity);
		List<MqttInboundMessage> snapshot = new ArrayList<>(messages);
		if (snapshot.size() <= effectiveLimit) {
			return List.copyOf(snapshot);
		}
		return Collections.unmodifiableList(snapshot.subList(0, effectiveLimit));
	}

	public Instant lastMessageAt() {
		return lastMessageAt.get();
	}

	public int size() {
		return count.get();
	}

	public synchronized void clear() {
		messages.clear();
		count.set(0);
		lastMessageAt.set(null);
	}

	private static MqttInboundMessage truncatePayload(MqttInboundMessage message) {
		if (message == null || message.payload() == null || message.payload().length() <= MAX_PAYLOAD_LENGTH) {
			return message;
		}
		return new MqttInboundMessage(
				message.topic(),
				message.payload().substring(0, MAX_PAYLOAD_LENGTH),
				message.receivedAt(),
				message.qos());
	}
}
