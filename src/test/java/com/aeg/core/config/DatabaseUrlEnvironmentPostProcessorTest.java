package com.aeg.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

	@Test
	void buildsJdbcUrlFromDigitalOceanDatabaseUrl() {
		Map<String, Object> overrides = DatabaseUrlEnvironmentPostProcessor.buildDatasourceOverrides(
				"postgresql://doadmin:secret@db-postgresql-nyc1-16442-do-user-35225255-0.l.db.ondigitalocean.com:25060/defaultdb");

		assertThat(overrides).containsEntry(
				"spring.datasource.url",
				"jdbc:postgresql://db-postgresql-nyc1-16442-do-user-35225255-0.l.db.ondigitalocean.com:25060/defaultdb?sslmode=require");
		assertThat(overrides).containsEntry("spring.datasource.username", "doadmin");
		assertThat(overrides).containsEntry("spring.datasource.password", "secret");
	}

	@Test
	void preservesExistingJdbcUrl() {
		Map<String, Object> overrides = DatabaseUrlEnvironmentPostProcessor.buildDatasourceOverrides(
				"jdbc:postgresql://example.com:5432/appdb?sslmode=require");

		assertThat(overrides).containsEntry(
				"spring.datasource.url",
				"jdbc:postgresql://example.com:5432/appdb?sslmode=require");
	}
}