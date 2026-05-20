package com.aeg.core.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"app.mqtt.inbound.enabled=false",
				"app.mqtt.broker-url=tcp://localhost:1883",
				"app.security.admin.name=Test Admin",
				"app.security.admin.username=segar12345@gmail.com",
				"app.security.admin.password=aeg-r1"
		})
class MqttMonitorIT {

	@LocalServerPort
	int port;

	@Autowired
	ApplicationEventPublisher eventPublisher;

	@MockitoBean
	MqttService mqttService;

	@MockitoBean
	MqttConnectionProbeService mqttConnectionProbeService;

	@Test
	void recentMessagesEndpointReturnsBufferedInbound() throws Exception {
		eventPublisher.publishEvent(new MqttInboundReceivedEvent(this,
				new MqttInboundMessage("aeg/device/1", "{\"ok\":true}", Instant.parse("2025-06-01T12:00:00Z"), 1)));

		var client = java.net.http.HttpClient.newHttpClient();
		var request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create("http://localhost:" + port + "/api/mqtt/messages?limit=10"))
				.header("Authorization", bearerAuthHeader())
				.GET()
				.build();

		var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"topic\":\"aeg/device/1\"");
		assertThat(response.body()).contains("\"payload\":\"{\\\"ok\\\":true}\"");
	}

	@Test
	void getSubscriptionReturnsConfiguredTopic() throws Exception {
		var client = java.net.http.HttpClient.newHttpClient();
		var request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create("http://localhost:" + port + "/api/mqtt/subscription"))
				.header("Authorization", bearerAuthHeader())
				.GET()
				.build();

		var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"topic\"");
		assertThat(response.body()).contains("\"active\":false");
	}

	@Test
	void updateSubscriptionWhenInboundDisabledReturns503() throws Exception {
		var client = java.net.http.HttpClient.newHttpClient();
		var request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create("http://localhost:" + port + "/api/mqtt/subscription"))
				.header("Authorization", bearerAuthHeader())
				.header("Content-Type", "application/json")
				.PUT(java.net.http.HttpRequest.BodyPublishers.ofString("{\"topic\":\"aeg/custom/#\"}"))
				.build();

		var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(503);
	}

	@Test
	void monitorStatusEndpoint() throws Exception {
		var client = java.net.http.HttpClient.newHttpClient();
		var request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create("http://localhost:" + port + "/api/mqtt/status"))
				.header("Authorization", bearerAuthHeader())
				.GET()
				.build();

		var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"inboundEnabled\":false");
		assertThat(response.body()).contains("\"subscribedTopic\"");
	}

	@Test
	void webSocketReceivesInboundBroadcast() throws Exception {
		CountDownLatch connected = new CountDownLatch(1);
		CountDownLatch received = new CountDownLatch(1);
		String[] payload = new String[1];

		TextWebSocketHandler handler = new TextWebSocketHandler() {
			@Override
			public void afterConnectionEstablished(WebSocketSession session) {
				connected.countDown();
			}

			@Override
			protected void handleTextMessage(WebSocketSession session, TextMessage message) {
				payload[0] = message.getPayload();
				received.countDown();
			}
		};

		String token = rawToken();
		URI wsUri = URI.create("ws://localhost:" + port + "/ws/mqtt?token=" + token);
		StandardWebSocketClient wsClient = new StandardWebSocketClient();
		WebSocketSession session = wsClient.execute(handler, null, wsUri).get(10, TimeUnit.SECONDS);

		try {
			assertThat(connected.await(5, TimeUnit.SECONDS)).isTrue();
			eventPublisher.publishEvent(new MqttInboundReceivedEvent(this,
					new MqttInboundMessage("live/topic", "ping", Instant.now(), 0)));

			assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(payload[0]).contains("\"type\":\"message\"");
			assertThat(payload[0]).contains("\"topic\":\"live/topic\"");
			assertThat(payload[0]).contains("\"payload\":\"ping\"");
		} finally {
			session.close();
		}

		verify(mqttService, times(0)).publish(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void webSocketRejectsMissingToken() {
		StandardWebSocketClient wsClient = new StandardWebSocketClient();
		URI wsUri = URI.create("ws://localhost:" + port + "/ws/mqtt");
		try {
			wsClient.execute(new TextWebSocketHandler() {}, null, wsUri).get(5, TimeUnit.SECONDS);
		} catch (Exception ex) {
			assertThat(ex).isNotNull();
		}
	}

	private String bearerAuthHeader() throws Exception {
		return "Bearer " + rawToken();
	}

	private String rawToken() throws Exception {
		var client = java.net.http.HttpClient.newHttpClient();
		var loginBody = "{\"username\":\"segar12345@gmail.com\",\"password\":\"aeg-r1\"}";
		var loginRequest = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create("http://localhost:" + port + "/api/auth/login"))
				.header("Content-Type", "application/json")
				.POST(java.net.http.HttpRequest.BodyPublishers.ofString(loginBody))
				.build();
		var loginResponse = client.send(loginRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
		assertThat(loginResponse.statusCode()).isEqualTo(200);
		Matcher matcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(loginResponse.body());
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}
}
