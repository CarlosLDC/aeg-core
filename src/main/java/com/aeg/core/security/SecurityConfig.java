package com.aeg.core.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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

	private final JwtAuthenticationFilter jwtAuthFilter;

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.exceptionHandling(eh -> eh.authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/api/auth/login").permitAll()
				.requestMatchers("/api/auth/me").authenticated()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").authenticated()
				.requestMatchers(HttpMethod.POST, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/clients/**").hasAnyRole("ADMIN", "DISTRIBUTOR")
				.requestMatchers(HttpMethod.DELETE, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/employees/**").authenticated()
				.requestMatchers(HttpMethod.POST, "/api/employees/**").authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/employees/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
				.requestMatchers("/api/software/**", "/api/printer-models/**", "/api/mqtt/**").hasRole("ADMIN")
				.requestMatchers("/api/distributor-contracts/**", "/api/service-center-contracts/**").hasRole("ADMIN")
				.requestMatchers("/api/distributor-persons/**").hasAnyRole("ADMIN", "DISTRIBUTOR")
				.requestMatchers("/api/printers/**").hasAnyRole("ADMIN", "DISTRIBUTOR", "TECHNICIAN")
				.requestMatchers("/api/seals/**", "/api/technical-services/**", "/api/annual-inspections/**", "/api/technicians/**").hasAnyRole("ADMIN", "TECHNICIAN", "SERVICE_CENTER")
				.requestMatchers("/error").permitAll()
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(buildAllowedOriginPatterns());
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	/**
	 * Patrones (no lista fija de orígenes) para soportar www, previews de Vercel y localhost.
	 * Con allowCredentials=true Spring recomienda allowedOriginPatterns en lugar de allowedOrigins.
	 */
	private List<String> buildAllowedOriginPatterns() {
		Set<String> patterns = new LinkedHashSet<>();
		patterns.add("http://localhost:*");
		patterns.add("http://127.0.0.1:*");
		patterns.add("https://aeg-admin.tech");
		patterns.add("https://www.aeg-admin.tech");
		patterns.add("https://*.aeg-admin.tech");
		patterns.add("https://aeg-core-admin.vercel.app");
		patterns.add("https://*.vercel.app");
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
