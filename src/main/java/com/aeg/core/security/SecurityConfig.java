package com.aeg.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.exceptionHandling(eh -> eh.authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/auth/login").permitAll()
				.requestMatchers("/api/auth/me").authenticated()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").authenticated()
				.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").authenticated()
				.requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").hasRole("ADMIN")
				.requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/companies/**", "/api/distributors/**", "/api/service-centers/**", "/api/branches/**", "/api/clients/**").hasRole("ADMIN")
				.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/employees/**").authenticated()
				.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/employees/**").authenticated()
				.requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/employees/**").hasRole("ADMIN")
				.requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
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
	org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
		org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
		configuration.setAllowedOrigins(java.util.Arrays.asList(
			"http://localhost:3000", 
			"http://localhost:5173", 
			"http://localhost:4200"
		));
		configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(java.util.Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
		configuration.setAllowCredentials(true);
		org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}