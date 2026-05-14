package com.aeg.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/auth/**").permitAll()
				.requestMatchers("/error").permitAll()
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.anyRequest().hasRole("ADMIN")
			)
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
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

	@Bean
	UserDetailsService userDetailsService(
		@Value("${app.security.admin.username:segar12345@gmail.com}") String adminUsername,
		@Value("${app.security.admin.password:aeg-r1}") String adminPassword,
		PasswordEncoder passwordEncoder
	) {
		return new InMemoryUserDetailsManager(
			User.withUsername(adminUsername)
				.password(passwordEncoder.encode(adminPassword))
				.roles("ADMIN")
				.build()
		);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}