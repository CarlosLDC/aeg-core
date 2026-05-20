package com.aeg.core.mqtt.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.aeg.core.mqtt.MqttInboundMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MqttMonitorSessionRegistry {

	private final ObjectMapper objectMapper;
	private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

	public void register(WebSocketSession session) {
		sessions.add(session);
	}

	public void unregister(WebSocketSession session) {
		sessions.remove(session);
	}

	public void broadcastSubscriptionChanged(String topic) {
		broadcastJson(MqttMonitorWireMessage.subscriptionChanged(topic));
	}

	public void broadcast(MqttInboundMessage message) {
		if (sessions.isEmpty()) {
			return;
		}
		broadcastJson(MqttMonitorWireMessage.fromInbound(message));
	}

	private void broadcastJson(Object wireMessage) {
		if (sessions.isEmpty()) {
			return;
		}
		String payload;
		try {
			payload = objectMapper.writeValueAsString(wireMessage);
		} catch (IOException ex) {
			log.warn("Could not serialize MQTT monitor message: {}", ex.getMessage());
			return;
		}
		for (WebSocketSession session : sessions) {
			if (!session.isOpen()) {
				sessions.remove(session);
				continue;
			}
			try {
				synchronized (session) {
					session.sendMessage(new TextMessage(payload));
				}
			} catch (IOException ex) {
				log.debug("WebSocket send failed for session {}: {}", session.getId(), ex.getMessage());
				sessions.remove(session);
			}
		}
	}
}
