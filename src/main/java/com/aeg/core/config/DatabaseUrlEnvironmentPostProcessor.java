package com.aeg.core.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String PROPERTY_SOURCE_NAME = "aeg-core-database-url";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String explicitUrl = environment.getProperty("SPRING_DATASOURCE_URL");
		if (explicitUrl != null && !explicitUrl.isBlank()) {
			return;
		}

		String databaseUrl = environment.getProperty("DATABASE_URL");
		if (databaseUrl == null || databaseUrl.isBlank()) {
			return;
		}

		Map<String, Object> overrides = buildDatasourceOverrides(databaseUrl);
		if (!overrides.isEmpty()) {
			environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
		}
	}

	static Map<String, Object> buildDatasourceOverrides(String databaseUrl) {
		Map<String, Object> overrides = new LinkedHashMap<>();

		URI uri = URI.create(databaseUrl);
		if (uri.getScheme() == null) {
			return overrides;
		}

		if (databaseUrl.startsWith("jdbc:")) {
			overrides.put("spring.datasource.url", databaseUrl);
			return overrides;
		}

		String host = uri.getHost();
		if (host == null || host.isBlank()) {
			return overrides;
		}

		StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
				.append(host);

		if (uri.getPort() != -1) {
			jdbcUrl.append(":").append(uri.getPort());
		}

		String path = uri.getPath();
		if (path != null && !path.isBlank()) {
			jdbcUrl.append(path);
		}

		Map<String, String> userInfo = parseUserInfo(uri.getUserInfo());
		String query = uri.getQuery();
		if (query == null || query.isBlank()) {
			query = "sslmode=require";
		} else if (!query.contains("sslmode=")) {
			query = query + "&sslmode=require";
		}

		jdbcUrl.append('?').append(query);

		overrides.put("spring.datasource.url", jdbcUrl.toString());
		if (userInfo.containsKey("username")) {
			overrides.put("spring.datasource.username", userInfo.get("username"));
		}
		if (userInfo.containsKey("password")) {
			overrides.put("spring.datasource.password", userInfo.get("password"));
		}

		return overrides;
	}

	static Map<String, String> parseUserInfo(String userInfo) {
		Map<String, String> credentials = new LinkedHashMap<>();
		if (userInfo == null || userInfo.isBlank()) {
			return credentials;
		}

		String decodedUserInfo = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
		String[] parts = decodedUserInfo.split(":", 2);
		if (parts.length > 0 && !parts[0].isBlank()) {
			credentials.put("username", parts[0]);
		}
		if (parts.length > 1 && !parts[1].isBlank()) {
			credentials.put("password", parts[1]);
		}
		return credentials;
	}
}