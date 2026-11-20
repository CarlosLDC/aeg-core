package com.aeg.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
@ConditionalOnProperty(name = "app.security.swagger-enabled", havingValue = "true")
public class OpenApiConfig {

    @Bean
    public OpenAPI aegCoreOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("AEG Core API")
                .description("Documentación de los endpoints para el sistema AEG Core")
                .version("v0.0.1")
                .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
