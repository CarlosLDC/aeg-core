package com.aeg.core.mqtt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageHistory {

	private final int capacity;
	private final Deque<MqttInboundMessage> messages = new ConcurrentLinkedDeque<>();
	private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>();

	public MqttMessageHistory(@Value("${app.mqtt.monitor.buffer-size:200}") int capacity) {
		this.capacity = Math.max(1, capacity);
	}

	public void add(MqttInboundMessage message) {
		lastMessageAt.set(message.receivedAt());
		messages.addFirst(message);
		while (messages.size() > capacity) {
			messages.pollLast();
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
		return messages.size();
	}

	public void clear() {
		messages.clear();
		lastMessageAt.set(null);
	}
}
