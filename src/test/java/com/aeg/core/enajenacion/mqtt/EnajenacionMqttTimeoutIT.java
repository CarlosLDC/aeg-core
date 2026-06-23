package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;
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
        "app.mqtt.enajenacion.enabled=true",
        "app.mqtt.enajenacion.skip-registration-status=true",
        "app.mqtt.enajenacion.timeout.dnf-seconds=2",
        "app.mqtt.enajenacion.timeout.fiscal-rif-seconds=2",
        "app.mqtt.enajenacion.timeout.config-seconds=2",
        "app.mqtt.enajenacion.timeout.invoice-seconds=2",
        "app.mqtt.enajenacion.timeout.credit-note-seconds=2",
        "app.mqtt.enajenacion.timeout.report-z-seconds=2"
})
class EnajenacionMqttTimeoutIT {

    private static final String MAC = "AA:BB:CC:DD:EE:05";

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

    @Autowired
    EnajenacionSessionRegistry sessionRegistry;

    @MockitoBean
    MqttService mqttService;

    @MockitoBean
    MqttConnectionProbeService mqttConnectionProbeService;

    @Test
    void dnfTimeoutAbortsSessionWithoutMarkingEnajenada() throws InterruptedException {
        var fixture = EnajenacionTestData.seedAssignedPrinter(
                companyRepository,
                branchRepository,
                clientRepository,
                modelRepository,
                softwareRepository,
                printerRepository,
                "GRA0000006",
                MAC,
                PrinterStatus.ASIGNADA);

        sendPtrEnajenar(fixture.compactMac(), EnajenacionMqttResponses.ptrEnajenar(fixture.fiscalSerial(), fixture.colonMac()));

        verify(mqttService, times(1)).publish(eq(fixture.comandoTopic()), org.mockito.ArgumentMatchers.anyString());
        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isTrue();

        Thread.sleep(2_500L);

        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isFalse();
        assertThat(printerRepository.findById(fixture.printer().getId()).orElseThrow().getStatus())
                .isEqualTo(PrinterStatus.ASIGNADA);
        verifyNoMoreInteractions(mqttService);
    }

    @Test
    void fiscalRifTimeoutAbortsSessionWithoutMarkingEnajenada() throws InterruptedException {
        var fixture = EnajenacionTestData.seedAssignedPrinter(
                companyRepository,
                branchRepository,
                clientRepository,
                modelRepository,
                softwareRepository,
                printerRepository,
                "GRA0000007",
                MAC,
                PrinterStatus.ASIGNADA);

        sendPtrEnajenar(fixture.compactMac(), EnajenacionMqttResponses.ptrEnajenar(fixture.fiscalSerial(), fixture.colonMac()));
        sendDeviceResponse(fixture.compactMac(), EnajenacionMqttResponses.dnfSuccess());

        verify(mqttService, times(2)).publish(eq(fixture.comandoTopic()), org.mockito.ArgumentMatchers.anyString());
        assertThat(sessionRegistry.find(fixture.compactMac()).orElseThrow().state())
                .isEqualTo(EnajenacionSessionState.FISCAL_RIF_SENT);

        Thread.sleep(2_500L);

        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isFalse();
        assertThat(printerRepository.findById(fixture.printer().getId()).orElseThrow().getStatus())
                .isEqualTo(PrinterStatus.ASIGNADA);
        verifyNoMoreInteractions(mqttService);
    }

    private void sendDeviceResponse(String compactMac, String payload) {
        String topic = FiscalMqttTopics.respuestaTopic(compactMac);
        eventPublisher.publishEvent(new MqttInboundReceivedEvent(
                this, new MqttInboundMessage(topic, payload, Instant.now(), 1)));
    }

    private void sendPtrEnajenar(String compactMac, String payload) {
        String topic = "/" + compactMac + "/AEG_Fiscal/Integracion/CmdServer";
        eventPublisher.publishEvent(new MqttInboundReceivedEvent(
                this, new MqttInboundMessage(topic, payload, Instant.now(), 1)));
    }
}
