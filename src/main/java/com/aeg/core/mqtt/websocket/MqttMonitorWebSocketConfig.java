package com.aeg.core.mqtt.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class MqttMonitorWebSocketConfig implements WebSocketConfigurer {

	private final MqttMonitorWebSocketHandler mqttMonitorWebSocketHandler;
	private final MqttWebSocketAuthInterceptor mqttWebSocketAuthInterceptor;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(mqttMonitorWebSocketHandler, "/ws/mqtt")
				.addInterceptors(mqttWebSocketAuthInterceptor)
				.setAllowedOrigins("*");
	}
}
