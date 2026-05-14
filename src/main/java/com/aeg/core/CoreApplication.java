package com.aeg.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class CoreApplication {

	private static final Logger logger = LoggerFactory.getLogger(CoreApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(CoreApplication.class, args);
		Environment env = context.getBean(Environment.class);
		
		String url = env.getProperty("spring.datasource.url");
		String user = env.getProperty("spring.datasource.username");
		
		logger.info("--- DIAGNÓSTICO DE CONEXIÓN ---");
		logger.info("Database URL: {}", url != null ? url.replaceAll(":.*@", ":***@") : "null");
		logger.info("Database User: {}", user);
		logger.info("SSL Mode: {}", env.getProperty("spring.datasource.hikari.data-source-properties.sslmode"));
		logger.info("-------------------------------");
	}

}
