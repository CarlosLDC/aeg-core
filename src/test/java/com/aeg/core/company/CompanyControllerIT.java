package com.aeg.core.company;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompanyControllerIT {

    @LocalServerPort
    int port;

    @Test
    void createAndListCompany() throws Exception {
        String body = "{\"businessName\":\"ACME SA\",\"rif\":\"V12345678\",\"contributorType\":\"Ordinario\"}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/companies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(201);
        assertThat(res.body()).contains("V12345678");

        HttpRequest listReq = HttpRequest.newBuilder().uri(java.net.URI.create("http://localhost:" + port + "/api/companies")).GET().build();
        HttpResponse<String> listRes = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        assertThat(listRes.statusCode()).isEqualTo(200);
        assertThat(listRes.body()).contains("V12345678");
    }
}
