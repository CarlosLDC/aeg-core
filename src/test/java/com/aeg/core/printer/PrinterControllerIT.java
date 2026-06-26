package com.aeg.core.printer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final AtomicInteger SERIAL_SEQUENCE = new AtomicInteger(2000000);

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

    @Autowired
    PrinterRepository printerRepository;

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

    @Test
    void adminCanDisposeAssignedPaidPrinter() throws Exception {
        Client client = createClient();
        Printer printer = createPrinter(client, PrinterStatus.ASIGNADA, true);

        String body = sampleDispositionBody(client.getId());
        var res = postDispose(printer.getId(), body);

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("\"status\":\"asignada\"");
        assertThat(res.body()).contains("\"header\"");
        assertThat(res.body()).contains("\"trailer\"");
        Printer updated = printerRepository.findById(printer.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PrinterStatus.ASIGNADA);
        assertThat(updated.getClientId()).isEqualTo(client.getId());
        assertThat(updated.getInstallationDate()).isNotNull();
        assertThat(updated.getHeader()).isNotNull();
        assertThat(updated.getHeader().lines()).contains("CARACAS, DISTRITO CAPITAL");
        assertThat(updated.getTrailer()).isNotNull();
    }

    @Test
    void disposeRejectsUnpaidPrinter() throws Exception {
        Client client = createClient();
        Printer printer = createPrinter(client, PrinterStatus.ASIGNADA, false);

        var res = postDispose(printer.getId(), sampleDispositionBody(client.getId()));

        assertThat(res.statusCode()).isEqualTo(400);
        assertThat(printerRepository.findById(printer.getId()).orElseThrow().getStatus())
                .isEqualTo(PrinterStatus.ASIGNADA);
    }

    @Test
    void disposeRejectsPrinterThatIsNotAssigned() throws Exception {
        Client client = createClient();
        Printer printer = createPrinter(client, PrinterStatus.LABORATORIO, true);

        var res = postDispose(printer.getId(), sampleDispositionBody(client.getId()));

        assertThat(res.statusCode()).isEqualTo(400);
        assertThat(printerRepository.findById(printer.getId()).orElseThrow().getStatus())
                .isEqualTo(PrinterStatus.LABORATORIO);
    }

    @Test
    void disposeRejectsMissingClientId() throws Exception {
        Client client = createClient();
        Printer printer = createPrinter(client, PrinterStatus.ASIGNADA, true);

        var res = postDispose(printer.getId(), "{}");

        assertThat(res.statusCode()).isEqualTo(400);
        assertThat(printerRepository.findById(printer.getId()).orElseThrow().getStatus())
                .isEqualTo(PrinterStatus.ASIGNADA);
    }

    @Test
    void adminCanClearClientWithoutChangingDistributorAssignmentStatus() throws Exception {
        Client client = createClient();
        Printer printer = createPrinter(client, PrinterStatus.ASIGNADA, true);

        String body = "{"
                + "\"modelId\":" + printer.getModelId() + ","
                + "\"fiscalSerial\":\"" + printer.getFiscalSerial() + "\","
                + "\"paid\":" + printer.getPaid() + ","
                + "\"status\":\"asignada\","
                + "\"deviceType\":\"interno\","
                + "\"clientId\":null"
                + "}";

        var res = putPrinter(printer.getId(), body);

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("\"clientId\":null");
        Printer updated = printerRepository.findById(printer.getId()).orElseThrow();
        assertThat(updated.getClientId()).isNull();
        assertThat(updated.getStatus()).isEqualTo(PrinterStatus.ASIGNADA);
    }

    private static String sampleDispositionBody(Long clientId) {
        return "{"
                + "\"clientId\":" + clientId + ","
                + "\"header\":{\"lines\":[\"AV. PRINCIPAL\",\"CARACAS, DISTRITO CAPITAL\",\"CONTRIBUYENTE ORDINARIO\"]},"
                + "\"trailer\":{\"lines\":[\"PIE DE TICKET\"]}"
                + "}";
    }

    private java.net.http.HttpResponse<String> putPrinter(Long printerId, String body) throws Exception {
        var httpClient = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/printers/" + printerId))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private String basicAuthHeader() {
        String token = Base64.getEncoder()
                .encodeToString("segar12345@gmail.com:aeg-r1".getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private java.net.http.HttpResponse<String> postDispose(Long printerId, String body) throws Exception {
        var httpClient = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/printers/" + printerId + "/enajenar"))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private Client createClient() {
        Company company = new Company();
        company.setBusinessName("Disposition IT Co " + SERIAL_SEQUENCE.incrementAndGet());
        company.setRif("V" + SERIAL_SEQUENCE.get());
        company.setContributorType(ContributorType.ORDINARIO);
        company = companyRepository.save(company);

        Branch branch = new Branch();
        branch.setCompany(company);
        branch.setCity("Caracas");
        branch.setState("Distrito Capital");
        branch.setAddress("Av. Principal");
        branch = branchRepository.save(branch);

        Client client = new Client();
        client.setBranch(branch);
        return clientRepository.save(client);
    }

    private Printer createPrinter(Client client, PrinterStatus status, boolean paid) {
        PrinterModel model = new PrinterModel();
        model.setBrand("DispBrand");
        model.setModelCode("DSP-" + SERIAL_SEQUENCE.incrementAndGet());
        model.setPrice(new BigDecimal("0.00"));
        model = modelRepository.save(model);

        Printer printer = new Printer();
        printer.setModel(model);
        printer.setClient(client);
        printer.setFiscalSerial(nextFiscalSerial());
        printer.setFinalSalePrice(new BigDecimal("100.00"));
        printer.setPaid(paid);
        printer.setStatus(status);
        printer.setDeviceType(DeviceType.INTERNO);
        printer.setVersionFirmware("1.0.0");
        printer.setMacAddress(nextMacAddress());
        return printerRepository.save(printer);
    }

    private String nextFiscalSerial() {
        return "TST" + String.format("%07d", SERIAL_SEQUENCE.incrementAndGet() % 10000000);
    }

    private String nextMacAddress() {
        int value = SERIAL_SEQUENCE.incrementAndGet();
        return String.format("AA:BB:CC:%02X:%02X:%02X",
                (value >> 16) & 0xFF,
                (value >> 8) & 0xFF,
                value & 0xFF);
    }
}
