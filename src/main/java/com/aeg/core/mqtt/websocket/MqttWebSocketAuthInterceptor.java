package com.aeg.core.mqtt.websocket;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.aeg.core.security.AdminJwtTokenValidator;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MqttWebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_PARAM = "token";

    private final AdminJwtTokenValidator adminJwtTokenValidator;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String token = extractToken(request);
        var username = adminJwtTokenValidator.validateAdminToken(token);
        if (username.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("username", username.get());
        return true;
    }

	@Override
	public void afterHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler wsHandler,
			Exception exception) {
		// no-op
	}

    private static String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String queryToken = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken;
            }
        }
        return AdminJwtTokenValidator.extractTokenFromQueryOrHeader(
                null,
                request.getHeaders().get("Authorization"));
    }
}
