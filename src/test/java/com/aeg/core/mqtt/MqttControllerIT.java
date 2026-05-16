package com.aeg.core.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.mqtt.broker-url=tcp://localhost:1883",
        "app.mqtt.client-id=test-mqtt-client",
        "app.mqtt.default-topic=aeg/test",
        "app.security.admin.name=Test Admin",
        "app.security.admin.username=segar12345@gmail.com",
        "app.security.admin.password=aeg-r1"
    }
)
public class MqttControllerIT {

    @LocalServerPort
    int port;

        @MockitoBean
    MqttService mqttService;

        @MockitoBean
    MqttConnectionProbeService mqttConnectionProbeService;

    @Test
    void publishCustomJsonPayload() throws Exception {
        String body = "{"
                + "\"topic\":\"aeg/test/manual\"," 
                + "\"payload\":{"
                + "\"message\":\"hola desde test\"," 
                + "\"priority\":1,"
                + "\"tags\":[\"mqtt\",\"postman\"]"
                + "}"
                + "}";

        var httpClient = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/mqtt/publish"))
            .header("Authorization", bearerAuthHeader())
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).contains("\"status\":\"sent\"");
        assertThat(response.body()).contains("\"topic\":\"aeg/test/manual\"");
        assertThat(response.body()).contains("\"payload\":\"{\\\"message\\\":\\\"hola desde test\\\",\\\"priority\\\":1,\\\"tags\\\":[\\\"mqtt\\\",\\\"postman\\\"]}\"");

        verify(mqttService, times(1)).publish(
                eq("aeg/test/manual"),
                eq("{\"message\":\"hola desde test\",\"priority\":1,\"tags\":[\"mqtt\",\"postman\"]}"));
    }

            private String bearerAuthHeader() throws Exception {
            var client = java.net.http.HttpClient.newHttpClient();
            var loginBody = "{"
                + "\"username\":\"segar12345@gmail.com\"," 
                + "\"password\":\"aeg-r1\""
                + "}";

            var loginRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(loginBody))
                .build();

            var loginResponse = client.send(loginRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(loginResponse.statusCode()).isEqualTo(200);

            Matcher matcher = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"").matcher(loginResponse.body());
            assertThat(matcher.find()).isTrue();
            return "Bearer " + matcher.group(1);
    }
}