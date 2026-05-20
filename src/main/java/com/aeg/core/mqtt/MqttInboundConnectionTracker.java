package com.aeg.core.mqtt;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.stereotype.Component;

@Component
public class MqttInboundConnectionTracker {

	private final AtomicBoolean subscribed = new AtomicBoolean(false);
	private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>();

	@EventListener
	public void onSubscribed(MqttSubscribedEvent event) {
		subscribed.set(true);
	}

	@EventListener
	public void onConnectionFailed(MqttConnectionFailedEvent event) {
		subscribed.set(false);
	}

	public void markMessageReceived() {
		lastMessageAt.set(Instant.now());
		subscribed.set(true);
	}

	public boolean isSubscribed() {
		return subscribed.get();
	}

	public Instant lastMessageAt() {
		return lastMessageAt.get();
	}
}
