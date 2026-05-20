package com.aeg.core.mqtt;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.aeg.core.mqtt.websocket.MqttMonitorSessionRegistry;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MqttInboundMonitor implements ApplicationListener<MqttInboundReceivedEvent> {

	private final MqttMessageHistory history;
	private final MqttMonitorSessionRegistry sessionRegistry;
	private final MqttInboundConnectionTracker connectionTracker;

	@Override
	public void onApplicationEvent(MqttInboundReceivedEvent event) {
		MqttInboundMessage message = event.message();
		history.add(message);
		connectionTracker.markMessageReceived();
		sessionRegistry.broadcast(message);
	}
}
