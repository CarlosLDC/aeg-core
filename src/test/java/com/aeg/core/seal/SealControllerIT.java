package com.aeg.core.seal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aeg.core.seal.dto.SealRequest;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.OffsetDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SealControllerIT {

    @LocalServerPort
    int port;

    @Autowired
    PrinterRepository printerRepository;

    @Test
    void createAndGetSeal() throws Exception {
        Printer p = new Printer();
        p.setFiscalSerial("DEF1234567");
        p.setStatus(com.aeg.core.printer.PrinterStatus.LABORATORIO);
        p.setDeviceType(com.aeg.core.printer.DeviceType.INTERNO);
        p.setPaid(Boolean.FALSE);
        p = printerRepository.save(p);

        String body = "{"
            + "\"printerId\":" + p.getId() + ","
            + "\"serial\":\"SN-001\"," 
            + "\"installationDate\":null,"
            + "\"removalDate\":null,"
            + "\"color\":\"rojo\"," 
            + "\"status\":\"instalado\""
            + "}";

        var client = java.net.http.HttpClient.newHttpClient();
        var reqHttp = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("http://localhost:" + port + "/api/seals"))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
            .build();

        var res = client.send(reqHttp, java.net.http.HttpResponse.BodyHandlers.ofString());
        org.assertj.core.api.Assertions.assertThat(res.statusCode()).isEqualTo(201);
        org.assertj.core.api.Assertions.assertThat(res.body()).contains("SN-001");

        var listReq = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("http://localhost:" + port + "/api/seals"))
            .GET()
            .build();

        var listRes = client.send(listReq, java.net.http.HttpResponse.BodyHandlers.ofString());
        org.assertj.core.api.Assertions.assertThat(listRes.statusCode()).isEqualTo(200);
        org.assertj.core.api.Assertions.assertThat(listRes.body()).contains("SN-001");
    }
}
