package com.aeg.core.printer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
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
import com.aeg.core.printer.dto.PrinterRequest;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.software.Software;
import com.aeg.core.software.SoftwareRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PrinterControllerIT {

    @LocalServerPort
    int port;

    @Autowired
    PrinterModelRepository modelRepository;

    @Autowired
    SoftwareRepository softwareRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    ClientRepository clientRepository;

    @Test
    void createAndGetPrinter() throws Exception {
        Company company = new Company();
        company.setBusinessName("Printer IT Co");
        company.setRif("V55512340");
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

        PrinterModel model = new PrinterModel();
        model.setBrand("TestBrand");
        model.setModelCode("TST-1");
        model.setPrice(new BigDecimal("0.00"));
        model = modelRepository.save(model);

        Software sw = new Software();
        sw.setName("Test SW");
        sw.setVersion("1.0");
        sw = softwareRepository.save(sw);

        PrinterRequest req = new PrinterRequest(
                model.getId(),
                sw.getId(),
                testClient.getId(),
                null,
                "ABC1234567",
                new BigDecimal("100.00"),
                Boolean.FALSE,
                OffsetDateTime.now(),
                "1.0.0",
                "AA:BB:CC:DD:EE:FF",
                PrinterStatus.LABORATORIO,
                DeviceType.INTERNO);
        String body = "{"
                + "\"modelId\":" + req.modelId() + ","
                + (req.softwareId() != null ? "\"softwareId\":" + req.softwareId() + "," : "")
                + "\"clientId\":" + req.clientId() + ","
                + "\"fiscalSerial\":\"ABC1234567\","
                + "\"finalSalePrice\":100.00,"
                + "\"paid\":false,"
                + "\"installationDate\":\"" + req.installationDate() + "\","
                + "\"versionFirmware\":\"1.0.0\","
                + "\"macAddress\":\"AA:BB:CC:DD:EE:FF\","
                + "\"status\":\"LABORATORIO\","
                + "\"deviceType\":\"INTERNO\""
                + "}";

        var httpClient = java.net.http.HttpClient.newHttpClient();
        var reqHttp = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/printers"))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();

        var res = httpClient.send(reqHttp, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(201);
        assertThat(res.body()).contains("ABC1234567");
        assertThat(res.body()).contains("\"clientId\":" + testClient.getId());

        var listReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/printers"))
                .header("Authorization", basicAuthHeader())
                .GET()
                .build();

        var listRes = httpClient.send(listReq, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(listRes.statusCode()).isEqualTo(200);
        assertThat(listRes.body()).contains("ABC1234567");
    }

    private String basicAuthHeader() {
        String token = Base64.getEncoder()
                .encodeToString("segar12345@gmail.com:aeg-r1".getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
