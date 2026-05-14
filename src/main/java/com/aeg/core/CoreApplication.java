package com.aeg.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class CoreApplication {

	private static final Logger logger = LoggerFactory.getLogger(CoreApplication.class);

	public static void main(String[] args) {
		logger.info("--- DIAGNÓSTICO DE ENTORNO (PRE-ARRANQUE) ---");
		logger.info("SPRING_DATASOURCE_URL: {}", System.getenv("SPRING_DATASOURCE_URL") != null ? "PRESENTE" : "FALTANTE");
		logger.info("DB_HOST: {}", System.getenv("DB_HOST"));
		logger.info("DB_PORT: {}", System.getenv("DB_PORT"));
		logger.info("DB_SSL_MODE: {}", System.getenv("DB_SSL_MODE"));
		logger.info("---------------------------------------------");
		
		SpringApplication.run(CoreApplication.class, args);
	}

}
