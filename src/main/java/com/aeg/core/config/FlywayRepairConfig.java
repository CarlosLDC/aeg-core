package com.aeg.core.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * One-shot repair for environments where migration files were edited after apply
 * (checksum mismatch on validate-on-migrate). Enable with APP_FLYWAY_REPAIR_ON_STARTUP=true,
 * deploy once, then remove the variable.
 */
@Configuration
@ConditionalOnProperty(name = "app.flyway.repair-on-startup", havingValue = "true")
@Slf4j
public class FlywayRepairConfig {

	@Bean
	FlywayMigrationStrategy repairThenMigrateStrategy() {
		return (Flyway flyway) -> {
			log.warn(
					"APP_FLYWAY_REPAIR_ON_STARTUP=true: running Flyway repair() before migrate(). "
							+ "Remove this env var after a successful deploy.");
			flyway.repair();
			flyway.migrate();
		};
	}
}
