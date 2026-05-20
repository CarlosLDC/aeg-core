package com.aeg.core.mqtt.websocket;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.aeg.core.security.JwtService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MqttWebSocketAuthInterceptor implements HandshakeInterceptor {

	private static final String TOKEN_PARAM = "token";

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	public boolean beforeHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		String token = extractToken(request);
		if (token == null || token.isBlank()) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		try {
			String username = jwtService.extractUsername(token);
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			if (!jwtService.isTokenValid(token, userDetails)) {
				response.setStatusCode(HttpStatus.UNAUTHORIZED);
				return false;
			}
			if (!isAdmin(userDetails)) {
				response.setStatusCode(HttpStatus.FORBIDDEN);
				return false;
			}
			attributes.put("username", username);
			return true;
		} catch (Exception ex) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
	}

	@Override
	public void afterHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler wsHandler,
			Exception exception) {
		// no-op
	}

	private static boolean isAdmin(UserDetails userDetails) {
		return userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch("ROLE_ADMIN"::equals);
	}

	private static String extractToken(ServerHttpRequest request) {
		if (request instanceof ServletServerHttpRequest servletRequest) {
			String queryToken = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
			if (queryToken != null && !queryToken.isBlank()) {
				return queryToken;
			}
		}
		List<String> authHeaders = request.getHeaders().get("Authorization");
		if (authHeaders != null) {
			for (String header : authHeaders) {
				if (header != null && header.startsWith("Bearer ")) {
					return header.substring(7);
				}
			}
		}
		return null;
	}
}
