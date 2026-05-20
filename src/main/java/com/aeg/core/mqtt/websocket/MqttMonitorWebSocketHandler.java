package com.aeg.core.mqtt.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MqttMonitorWebSocketHandler extends TextWebSocketHandler {

	private final MqttMonitorSessionRegistry sessionRegistry;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessionRegistry.register(session);
		log.debug("MQTT monitor WebSocket connected: {}", session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sessionRegistry.unregister(session);
		log.debug("MQTT monitor WebSocket closed: {} ({})", session.getId(), status);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		// v1: read-only feed; ignore client messages except optional ping
		if ("ping".equalsIgnoreCase(message.getPayload())) {
			try {
				session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
			} catch (Exception ex) {
				log.debug("Could not send pong: {}", ex.getMessage());
			}
		}
	}
}
