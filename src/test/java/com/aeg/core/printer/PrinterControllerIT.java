package com.aeg.core.printer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aeg.core.printer.dto.PrinterRequest;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.software.Software;
import com.aeg.core.software.SoftwareRepository;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PrinterControllerIT {

        @LocalServerPort
        int port;

        @Autowired
        PrinterModelRepository modelRepository;

        @Autowired
        SoftwareRepository softwareRepository;

        @Test
        void createAndGetPrinter() throws Exception {
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
                                null,
                                null,
                                "ABC1234567",
                                new BigDecimal("100.00"),
                                Boolean.FALSE,
                                OffsetDateTime.now(),
                                "1.0.0",
                                "AA:BB:CC:DD:EE:FF",
                                PrinterStatus.LABORATORIO,
                                DeviceType.INTERNO
                );
                String body = "{"
                        + "\"modelId\":" + req.modelId() + ","
                        + (req.softwareId() != null ? "\"softwareId\":" + req.softwareId() + "," : "")
                        + (req.branchId() != null ? "\"branchId\":" + req.branchId() + "," : "")
                        + "\"fiscalSerial\":\"ABC1234567\"," 
                        + "\"finalSalePrice\":100.00,"
                        + "\"paid\":false,"
                        + "\"versionFirmware\":\"1.0.0\","
                        + "\"macAddress\":\"AA:BB:CC:DD:EE:FF\","
                        + "\"status\":\"LABORATORIO\","
                        + "\"deviceType\":\"INTERNO\""
                        + "}";

                var client = java.net.http.HttpClient.newHttpClient();
                var reqHttp = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:" + port + "/api/printers"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                        .build();

                var res = client.send(reqHttp, java.net.http.HttpResponse.BodyHandlers.ofString());
                org.assertj.core.api.Assertions.assertThat(res.statusCode()).isEqualTo(201);
                org.assertj.core.api.Assertions.assertThat(res.body()).contains("ABC1234567");

                var listReq = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:" + port + "/api/printers"))
                        .GET()
                        .build();

                var listRes = client.send(listReq, java.net.http.HttpResponse.BodyHandlers.ofString());
                org.assertj.core.api.Assertions.assertThat(listRes.statusCode()).isEqualTo(200);
                org.assertj.core.api.Assertions.assertThat(listRes.body()).contains("ABC1234567");
    }
}
