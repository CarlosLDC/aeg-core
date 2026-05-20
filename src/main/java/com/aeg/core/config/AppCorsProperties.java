package com.aeg.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppCorsProperties {

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Value("${app.security.strict-cors:true}")
	private boolean strictCors;

	public List<String> allowedOriginPatterns() {
		Set<String> patterns = new LinkedHashSet<>();
		if (!strictCors) {
			patterns.add("http://localhost:*");
			patterns.add("http://127.0.0.1:*");
		}
		parseAllowedOrigins().forEach(patterns::add);
		return new ArrayList<>(patterns);
	}

	private List<String> parseAllowedOrigins() {
		return Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();
	}
}
