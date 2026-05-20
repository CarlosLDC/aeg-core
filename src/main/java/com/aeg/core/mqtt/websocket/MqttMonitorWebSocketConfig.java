package com.aeg.core.mqtt.websocket;

import com.aeg.core.config.AppCorsProperties;

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
	private final AppCorsProperties corsProperties;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(mqttMonitorWebSocketHandler, "/ws/mqtt")
				.addInterceptors(mqttWebSocketAuthInterceptor)
				.setAllowedOriginPatterns(
						corsProperties.allowedOriginPatterns().toArray(String[]::new));
	}
}
