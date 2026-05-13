package com.aeg.core.distributor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DistributorControllerIT {

    @LocalServerPort
    int port;

    @Test
    void createAndListDistributor() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // create company
        String company = "{\"businessName\":\"ACME SA\",\"rif\":\"V22345678\",\"contributorType\":\"Ordinario\"}";
        HttpRequest creCompany = HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/companies"))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(company))
                .build();
        HttpResponse<String> resCompany = client.send(creCompany, HttpResponse.BodyHandlers.ofString());
        assertThat(resCompany.statusCode()).isEqualTo(201);
        String location = resCompany.headers().firstValue("location").orElseThrow();
        // extract company id
        String[] parts = location.split("/");
        String companyId = parts[parts.length - 1];

        // create branch
        String branch = "{\"companyId\":" + companyId + ",\"city\":\"City\",\"state\":\"State\"}";
        HttpRequest creBranch = HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/branches"))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(branch))
                .build();
        HttpResponse<String> resBranch = client.send(creBranch, HttpResponse.BodyHandlers.ofString());
        assertThat(resBranch.statusCode()).isEqualTo(201);
        String branchId = extractId(resBranch.body());

        // create distributor referencing branch
        String distributor = "{\"branchId\":" + branchId + "}";
        HttpRequest creDist = HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/distributors"))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(distributor))
                .build();
        HttpResponse<String> resDist = client.send(creDist, HttpResponse.BodyHandlers.ofString());
        assertThat(resDist.statusCode()).isEqualTo(201);
        assertThat(resDist.body()).contains("branchId");

        // list distributors
                HttpRequest listReq = HttpRequest.newBuilder()
                                .uri(java.net.URI.create("http://localhost:" + port + "/api/distributors"))
                                .header("Authorization", basicAuthHeader())
                                .GET()
                                .build();
        HttpResponse<String> listRes = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        assertThat(listRes.statusCode()).isEqualTo(200);
        assertThat(listRes.body()).contains(branchId);
    }

        private String basicAuthHeader() {
                String token = Base64.getEncoder().encodeToString("segar12345@gmail.com:aeg-r1".getBytes(StandardCharsets.UTF_8));
                return "Basic " + token;
        }

        private String extractId(String body) {
                Matcher matcher = Pattern.compile("\\\"id\\\":(\\d+)").matcher(body);
                assertThat(matcher.find()).isTrue();
                return matcher.group(1);
        }
}
