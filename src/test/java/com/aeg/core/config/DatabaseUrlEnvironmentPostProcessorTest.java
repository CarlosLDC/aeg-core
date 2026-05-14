package com.aeg.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

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

	@Test
	void populatesSpringDatasourceUrlFromDatabaseUrlWhenNoExplicitDatasourceUrlExists() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DATABASE_URL", "postgresql://doadmin:secret@db.example.com:25060/defaultdb");

		new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://db.example.com:25060/defaultdb?sslmode=require");
		assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("doadmin");
		assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("secret");
	}
}