package com.aeg.core.seal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.Client;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.company.ContributorType;
import com.aeg.core.printer.DeviceType;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SealControllerIT {

    @LocalServerPort
    int port;

    @Autowired
    PrinterRepository printerRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    ClientRepository clientRepository;

    @Test
    void createAndGetSeal() throws Exception {
        Company company = new Company();
        company.setBusinessName("Seal IT Co");
        company.setRif("V55512341");
        company.setContributorType(ContributorType.ORDINARIO);
        company = companyRepository.save(company);

        Branch branch = new Branch();
        branch.setCompany(company);
        branch.setCity("City");
        branch.setState("State");
        branch = branchRepository.save(branch);

        Client testClient = new Client();
        testClient.setBranch(branch);
        testClient = clientRepository.save(testClient);

        Printer p = new Printer();
        p.setFiscalSerial("DEF1234567");
        p.setStatus(PrinterStatus.LABORATORIO);
        p.setDeviceType(DeviceType.INTERNO);
        p.setPaid(Boolean.FALSE);
        p.setClient(testClient);
        p = printerRepository.save(p);

        String body = "{"
                + "\"printerId\":" + p.getId() + ","
                + "\"serial\":\"SN-001\","
                + "\"installationDate\":null,"
                + "\"removalDate\":null,"
                + "\"color\":\"azul\","
                + "\"status\":\"disponible\""
                + "}";

        var httpClient = java.net.http.HttpClient.newHttpClient();
        var reqHttp = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/seals"))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();

        var res = httpClient.send(reqHttp, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(201);
        assertThat(res.body()).contains("SN-001");

        var listReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/seals"))
                .header("Authorization", basicAuthHeader())
                .GET()
                .build();

        var listRes = httpClient.send(listReq, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(listRes.statusCode()).isEqualTo(200);
        assertThat(listRes.body()).contains("SN-001");
    }

    private String basicAuthHeader() {
        String token = Base64.getEncoder()
                .encodeToString("segar12345@gmail.com:aeg-r1".getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
