package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIT {

	@LocalServerPort
	int port;

	@Test
	void rejectsRequestsWithoutCredentials() throws Exception {
		var client = java.net.http.HttpClient.newHttpClient();
		var request = java.net.http.HttpRequest.newBuilder()
			.uri(java.net.URI.create("http://localhost:" + port + "/api/companies"))
			.GET()
			.build();

		var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(401);
	}

	@Test
	void allowsAdminToAccessApis() throws Exception {
		var client = java.net.http.HttpClient.newHttpClient();
		var request = java.net.http.HttpRequest.newBuilder()
			.uri(java.net.URI.create("http://localhost:" + port + "/api/companies"))
			.header("Authorization", basicAuthHeader())
			.GET()
			.build();

		var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(200);
	}

	private String basicAuthHeader() {
		String token = Base64.getEncoder().encodeToString("segar12345@gmail.com:aeg-r1".getBytes(StandardCharsets.UTF_8));
		return "Basic " + token;
	}
}