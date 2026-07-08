package com.aeg.core.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.aeg.core.config.AppCorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] BOOK_READ_ROLES = {
			"ADMIN", "DISTRIBUTOR", "TECHNICIAN", "SERVICE_CENTER", "SENIAT"
	};

	private static final String[] BOOK_WRITE_ROLES = {
			"ADMIN", "DISTRIBUTOR"
	};

	private static final String[] TECHNICAL_SERVICE_WRITE_ROLES = {
			"ADMIN", "TECHNICIAN", "SERVICE_CENTER"
	};

	private static final String[] ANNUAL_INSPECTION_WRITE_ROLES = {
			"ADMIN", "DISTRIBUTOR", "TECHNICIAN", "SERVICE_CENTER"
	};

	private static final String[] TOOLS_MQTT_ROLES = {
			"ADMIN", "DISTRIBUTOR", "TECHNICIAN", "SERVICE_CENTER"
	};

	private static final String[] PANEL_ROLES = {
			"ADMIN", "DISTRIBUTOR"
	};

	private static final String[] DISTRIBUTOR_PANEL_ROLES = {
			"DISTRIBUTOR"
	};

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final PortalAuthorizationFilter portalAuthorizationFilter;
	private final AppCorsProperties corsProperties;

	@Value("${app.security.swagger-enabled:false}")
	private boolean swaggerEnabled;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.exceptionHandling(eh -> eh.authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> {
				authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/api/auth/login").permitAll()
				.requestMatchers("/api/auth/me").authenticated();
				if (swaggerEnabled) {
					authorize.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
				} else {
					authorize.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN");
				}
				authorize
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").hasAnyRole(PANEL_ROLES)
				.requestMatchers(HttpMethod.POST, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").hasAnyRole(PANEL_ROLES)
				.requestMatchers(HttpMethod.PUT, "/api/companies/**", "/api/branches/**").hasAnyRole("ADMIN", "DISTRIBUTOR")
				.requestMatchers(HttpMethod.PUT, "/api/distributors/**", "/api/service-centers/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/clients/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/client-modification-requests/update", "/api/client-modification-requests/delete").hasAnyRole(DISTRIBUTOR_PANEL_ROLES)
				.requestMatchers(HttpMethod.GET, "/api/client-modification-requests/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/client-modification-requests/*/approve", "/api/client-modification-requests/*/reject").hasRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/client-modification-requests/*/cancel").hasAnyRole(DISTRIBUTOR_PANEL_ROLES)
				.requestMatchers("/api/mqtt/enajenacion/stream").permitAll()
				.requestMatchers("/api/mqtt/annual-inspection/**").hasAnyRole(ANNUAL_INSPECTION_WRITE_ROLES)
				.requestMatchers("/api/mqtt/tools/**").hasAnyRole(TOOLS_MQTT_ROLES)
				.requestMatchers("/api/software/**", "/api/mqtt/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/printer-models/**").hasAnyRole("ADMIN", "DISTRIBUTOR")
				.requestMatchers(HttpMethod.POST, "/api/printer-models/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/printer-models/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/printer-models/**").hasRole("ADMIN")
				.requestMatchers("/api/distributor-contracts/**", "/api/service-center-contracts/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/printers/**").hasAnyRole(BOOK_READ_ROLES)
				.requestMatchers(HttpMethod.POST, "/api/printers/*/enajenar").hasAnyRole("ADMIN", "DISTRIBUTOR")
				.requestMatchers(HttpMethod.POST, "/api/printers/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/printers/**").hasAnyRole("ADMIN", "DISTRIBUTOR")
				.requestMatchers(HttpMethod.DELETE, "/api/printers/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/seals/**").hasAnyRole(BOOK_READ_ROLES)
				.requestMatchers(HttpMethod.POST, "/api/seals/**").hasAnyRole(BOOK_WRITE_ROLES)
				.requestMatchers(HttpMethod.PUT, "/api/seals/**").hasAnyRole(BOOK_WRITE_ROLES)
				.requestMatchers(HttpMethod.DELETE, "/api/seals/**").hasAnyRole(BOOK_WRITE_ROLES)
				.requestMatchers(HttpMethod.GET, "/api/technical-services/**").hasAnyRole(BOOK_READ_ROLES)
				.requestMatchers(HttpMethod.POST, "/api/technical-services/**").hasAnyRole(TECHNICAL_SERVICE_WRITE_ROLES)
				.requestMatchers(HttpMethod.PUT, "/api/technical-services/**").hasAnyRole(TECHNICAL_SERVICE_WRITE_ROLES)
				.requestMatchers(HttpMethod.DELETE, "/api/technical-services/**").hasAnyRole(TECHNICAL_SERVICE_WRITE_ROLES)
				.requestMatchers(HttpMethod.GET, "/api/annual-inspections/**").hasAnyRole(BOOK_READ_ROLES)
				.requestMatchers(HttpMethod.POST, "/api/annual-inspections/**").hasAnyRole(ANNUAL_INSPECTION_WRITE_ROLES)
				.requestMatchers(HttpMethod.PUT, "/api/annual-inspections/**").hasAnyRole(ANNUAL_INSPECTION_WRITE_ROLES)
				.requestMatchers(HttpMethod.DELETE, "/api/annual-inspections/**").hasAnyRole(ANNUAL_INSPECTION_WRITE_ROLES)
				.requestMatchers(HttpMethod.GET, "/api/fiscal-books/**").hasAnyRole(BOOK_READ_ROLES)
				.requestMatchers(HttpMethod.POST, "/api/fiscal-books/lookup-inspection-by-qr")
						.hasAnyRole(BOOK_READ_ROLES)
				.requestMatchers("/error").permitAll()
				.requestMatchers("/ws/mqtt", "/ws/mqtt/**").permitAll()
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.anyRequest().authenticated();
			})
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterAfter(portalAuthorizationFilter, JwtAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
