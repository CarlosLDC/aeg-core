package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.mqtt.MqttConnectionProbeService;
import com.aeg.core.mqtt.MqttInboundMessage;
import com.aeg.core.mqtt.MqttInboundReceivedEvent;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.software.SoftwareRepository;

@SpringBootTest(properties = {
        "app.mqtt.inbound.enabled=false",
        "app.mqtt.enajenacion.enabled=true"
})
class EnajenacionMqttIT {

    private static final String MAC_1 = "20:6E:F1:88:4C:68";
    private static final String MAC_2 = "AA:BB:CC:DD:EE:01";
    private static final String MAC_3 = "AA:BB:CC:DD:EE:02";
    private static final String MAC_4 = "AA:BB:CC:DD:EE:03";
    private static final String MAC_5 = "AA:BB:CC:DD:EE:04";
    private static final String MAC_6 = "AA:BB:CC:DD:EE:06";

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    PrinterModelRepository modelRepository;

    @Autowired
    SoftwareRepository softwareRepository;

    @Autowired
    PrinterRepository printerRepository;

    @MockitoBean
    MqttService mqttService;

    @MockitoBean
    MqttConnectionProbeService mqttConnectionProbeService;

    @Test
    void fullEnajenacionFlowMarksPrinterEnajenada() {
        var fixture = EnajenacionTestData.seedAssignedPrinter(
                companyRepository,
                branchRepository,
                clientRepository,
                modelRepository,
                softwareRepository,
                printerRepository,
                "GRA0000001",
                MAC_1,
                PrinterStatus.ASIGNADA);

        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.ptrEnajenar(fixture.fiscalSerial(), fixture.colonMac()));
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.dnfSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.fiscalRifSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.wFileSpiffSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.wFileSpiffSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.staInfSuccess(fixture.fiscalSerial()));
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.invoiceSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.creditNoteSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.reportZSuccess());

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService, times(8)).publish(topicCaptor.capture(), payloadCaptor.capture());

        List<String> topics = topicCaptor.getAllValues();
        List<String> payloads = payloadCaptor.getAllValues();
        assertThat(topics).containsOnly(fixture.comandoTopic());
        assertThat(payloads.get(0)).contains("\"cmd\":\"aperDNF\"");
        assertThat(payloads.get(1)).contains("\"cmd\":\"fiscalAEG\"").contains("J-0000001");
        assertThat(payloads.get(2)).contains("paramFacSPIFF.json").contains("CONTRIBUYENTE ORDINARIO");
        assertThat(payloads.get(3)).contains("configSPIFFS.json");
        assertThat(payloads.get(4)).contains("\"cmd\":\"StaInf\"").contains("NroRegMa");
        assertThat(payloads.get(5)).contains("\"cmd\":\"proF\"");
        assertThat(payloads.get(6)).contains("\"cmd\":\"nroFacNC\"");
        assertThat(payloads.get(7)).contains("\"cmd\":\"genImpRepZ\"");

        var updated = printerRepository.findById(fixture.printer().getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(updated.getInstallationDate()).isNotNull();
    }

    @Test
    void fullEnajenacionFlowMarksLaboratorioPrinterEnajenada() {
        var fixture = EnajenacionTestData.seedAssignedPrinter(
                companyRepository,
                branchRepository,
                clientRepository,
                modelRepository,
                softwareRepository,
                printerRepository,
                "GRA0000007",
                MAC_6,
                PrinterStatus.LABORATORIO);

        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.ptrEnajenar(fixture.fiscalSerial(), fixture.colonMac()));
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.dnfSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.fiscalRifSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.wFileSpiffSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.wFileSpiffSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.staInfSuccess(fixture.fiscalSerial()));
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.invoiceSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.creditNoteSuccess());
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.reportZSuccess());

        verify(mqttService, times(8)).publish(eq(fixture.comandoTopic()), org.mockito.ArgumentMatchers.anyString());

        var updated = printerRepository.findById(fixture.printer().getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(updated.getInstallationDate()).isNotNull();
    }

    @Test
    void ptrEnajenarIgnoredWhenPrinterNotAssigned() {
        var fixture = EnajenacionTestData.seedAssignedPrinter(
                companyRepository,
                branchRepository,
                clientRepository,
                modelRepository,
                softwareRepository,
                printerRepository,
                "GRA0000002",
                MAC_2,
                PrinterStatus.SIN_ASIGNAR);

        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.ptrEnajenar(fixture.fiscalSerial(), fixture.colonMac()));

        verifyNoInteractions(mqttService);
        assertThat(printerRepository.findById(fixture.printer().getId()).orElseThrow().getStatus())
                .isEqualTo(PrinterStatus.SIN_ASIGNAR);
    }

    @Test
    void ptrEnajenarIgnoredWhenAlreadyEnajenada() {
        var fixture = EnajenacionTestData.seedAssignedPrinter(
                companyRepository,
                branchRepository,
                clientRepository,
                modelRepository,
                softwareRepository,
                printerRepository,
                "GRA0000003",
                MAC_3,
                PrinterStatus.ENAJENADA);

        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.ptrEnajenar(fixture.fiscalSerial(), fixture.colonMac()));

        verifyNoInteractions(mqttService);
    }

    @Test
    void ptrEnajenarRejectedWhenMacMismatch() {
        var fixture = EnajenacionTestData.seedAssignedPrinter(
                companyRepository,
                branchRepository,
                clientRepository,
                modelRepository,
                softwareRepository,
                printerRepository,
                "GRA0000004",
                MAC_4,
                PrinterStatus.ASIGNADA);

        sendInbound(
                fixture.compactMac(),
                EnajenacionMqttResponses.ptrEnajenar(fixture.fiscalSerial(), "FF:FF:FF:FF:FF:FF"));

        verifyNoInteractions(mqttService);
        assertThat(printerRepository.findById(fixture.printer().getId()).orElseThrow().getStatus())
                .isEqualTo(PrinterStatus.ASIGNADA);
    }

    @Test
    void flowAbortsWhenDeviceReturnsInvalidDnf() {
        var fixture = EnajenacionTestData.seedAssignedPrinter(
                companyRepository,
                branchRepository,
                clientRepository,
                modelRepository,
                softwareRepository,
                printerRepository,
                "GRA0000005",
                MAC_5,
                PrinterStatus.ASIGNADA);

        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.ptrEnajenar(fixture.fiscalSerial(), fixture.colonMac()));
        sendInbound(fixture.compactMac(), EnajenacionMqttResponses.dnfFailure());

        verify(mqttService, times(1)).publish(eq(fixture.comandoTopic()), org.mockito.ArgumentMatchers.anyString());
        assertThat(printerRepository.findById(fixture.printer().getId()).orElseThrow().getStatus())
                .isEqualTo(PrinterStatus.ASIGNADA);
    }

    private void sendInbound(String compactMac, String payload) {
        String topic = compactMac + "/AEG_Fiscal/Integracion/CmdServer";
        eventPublisher.publishEvent(new MqttInboundReceivedEvent(
                this, new MqttInboundMessage(topic, payload, Instant.now(), 1)));
    }
}
