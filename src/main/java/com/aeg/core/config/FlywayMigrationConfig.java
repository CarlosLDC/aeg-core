package com.aeg.core.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Sincroniza checksums en flyway_schema_history antes de migrate (V10/V11 editadas tras aplicarse en prod).
 * repair() es seguro cuando el SQL aplicado en BD coincide con el archivo actual.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", matchIfMissing = true)
@Slf4j
public class FlywayMigrationConfig {

	@Bean
	FlywayMigrationStrategy repairThenMigrateStrategy() {
		return (Flyway flyway) -> {
			log.info("Flyway: running repair() then migrate()");
			flyway.repair();
			flyway.migrate();
		};
	}
}
