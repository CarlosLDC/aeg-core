package com.aeg.core.fiscalizacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aeg.core.mqtt.MqttConnectionProbeService;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.seal.SealStatus;

@SpringBootTest(properties = {
        "app.mqtt.inbound.enabled=false",
        "app.mqtt.enajenacion.enabled=false",
        "app.mqtt.fiscalizacion.enabled=true",
        "app.mqtt.fiscalizacion.timeout.result-seconds=2"
})
class FiscalizacionMqttIT {

    private static final String MAC = "AA:BB:CC:DD:EE:F1";

    @Autowired
    FiscalizacionMqttOrchestrator orchestrator;

    @Autowired
    PrinterModelRepository modelRepository;

    @Autowired
    SealRepository sealRepository;

    @Autowired
    PrinterRepository printerRepository;

    @Autowired
    FiscalizacionSessionRegistry sessionRegistry;

    @MockitoBean
    MqttService mqttService;

    @MockitoBean
    MqttConnectionProbeService mqttConnectionProbeService;

    @BeforeEach
    void resetSessions() {
        for (var session : sessionRegistry.listActive()) {
            sessionRegistry.remove(session.compactMac());
        }
        org.mockito.Mockito.reset(mqttService);
    }

    @Test
    void happyPathCreatesPrinterSinAsignarAndAssignsSeal() {
        var fixture = FiscalizacionTestData.seed(
                modelRepository, sealRepository, "GRA0000101", MAC, "G1B0101");

        orchestrator.handleInbound(fixture.cmdServerTopic(), FiscalizacionTestData.ptrFiscalizar(
                fixture.ptrReg(), fixture.colonMac(), fixture.precintoNro(), "Azul", "1.1.0", "AEG-R1"));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq(fixture.comandoTopic()), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"code\":0");
        assertThat(payloadCaptor.getValue()).contains(FiscalizacionConstants.MSG_LISTA);
        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isTrue();

        orchestrator.handleInbound(fixture.respuestaTopic(), FiscalizacionTestData.resultSuccess());

        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isFalse();
        var printer = printerRepository.findAll().stream()
                .filter(p -> fixture.ptrReg().equalsIgnoreCase(p.getFiscalSerial()))
                .findFirst()
                .orElseThrow();
        assertThat(printer.getStatus()).isEqualTo(PrinterStatus.SIN_ASIGNAR);
        assertThat(printer.getMacAddress()).isEqualToIgnoringCase(fixture.colonMac());
        assertThat(printer.getVersionFirmware()).isEqualTo("1.1.0");
        assertThat(printer.getModelId()).isEqualTo(fixture.model().getId());

        SealStatus sealStatus = sealRepository.findById(fixture.seal().getId()).orElseThrow().getStatus();
        assertThat(sealStatus).isEqualTo(SealStatus.EN_IMPRESORA);
        assertThat(sealRepository.findById(fixture.seal().getId()).orElseThrow().getPrinterId())
                .isEqualTo(printer.getId());
    }

    @Test
    void validationRejectsDuplicateRegistro() {
        var fixture = FiscalizacionTestData.seed(
                modelRepository, sealRepository, "GRA0000102", "AA:BB:CC:DD:EE:F2", "G1B0102");
        FiscalizacionTestData.seedExistingPrinter(
                printerRepository, fixture.model(), fixture.ptrReg(), "AA:BB:CC:DD:EE:99");

        orchestrator.handleInbound(fixture.cmdServerTopic(), FiscalizacionTestData.ptrFiscalizar(
                fixture.ptrReg(), fixture.colonMac(), fixture.precintoNro(), "Azul", "1.1.0", "AEG-R1"));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq(fixture.comandoTopic()), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"code\":1");
        assertThat(payloadCaptor.getValue()).contains(FiscalizacionConstants.MSG_REGISTRO_EXISTE);
        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isFalse();
    }

    @Test
    void validationRejectsDuplicateMac() {
        var fixture = FiscalizacionTestData.seed(
                modelRepository, sealRepository, "GRA0000103", "AA:BB:CC:DD:EE:F3", "G1B0103");
        FiscalizacionTestData.seedExistingPrinter(
                printerRepository, fixture.model(), "GRA0000199", fixture.colonMac());

        orchestrator.handleInbound(fixture.cmdServerTopic(), FiscalizacionTestData.ptrFiscalizar(
                fixture.ptrReg(), fixture.colonMac(), fixture.precintoNro(), "Azul", "1.1.0", "AEG-R1"));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq(fixture.comandoTopic()), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains(FiscalizacionConstants.MSG_MAC_EXISTE);
    }

    @Test
    void validationRejectsMissingSeal() {
        var fixture = FiscalizacionTestData.seed(
                modelRepository, sealRepository, "GRA0000104", "AA:BB:CC:DD:EE:F4", "G1B0104");

        orchestrator.handleInbound(fixture.cmdServerTopic(), FiscalizacionTestData.ptrFiscalizar(
                fixture.ptrReg(), fixture.colonMac(), "MISSING99", "Azul", "1.1.0", "AEG-R1"));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq(fixture.comandoTopic()), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains(FiscalizacionConstants.MSG_PRECINTO_NO_EXISTE);
    }

    @Test
    void validationRejectsAssignedSeal() {
        var fixture = FiscalizacionTestData.seed(
                modelRepository, sealRepository, "GRA0000105", "AA:BB:CC:DD:EE:F5", "G1B0105");
        var existing = FiscalizacionTestData.seedExistingPrinter(
                printerRepository, fixture.model(), "GRA0000188", "AA:BB:CC:DD:EE:88");
        var seal = sealRepository.findById(fixture.seal().getId()).orElseThrow();
        seal.setStatus(SealStatus.EN_IMPRESORA);
        seal.setPrinter(existing);
        sealRepository.save(seal);

        orchestrator.handleInbound(fixture.cmdServerTopic(), FiscalizacionTestData.ptrFiscalizar(
                fixture.ptrReg(), fixture.colonMac(), fixture.precintoNro(), "Azul", "1.1.0", "AEG-R1"));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq(fixture.comandoTopic()), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains(FiscalizacionConstants.MSG_PRECINTO_ASIGNADO);
    }

    @Test
    void deviceErrorDoesNotCreatePrinter() {
        var fixture = FiscalizacionTestData.seed(
                modelRepository, sealRepository, "GRA0000106", "AA:BB:CC:DD:EE:F6", "G1B0106");

        orchestrator.handleInbound(fixture.cmdServerTopic(), FiscalizacionTestData.ptrFiscalizar(
                fixture.ptrReg(), fixture.colonMac(), fixture.precintoNro(), "Azul", "1.1.0", "AEG-R1"));
        verify(mqttService, times(1)).publish(eq(fixture.comandoTopic()), org.mockito.ArgumentMatchers.anyString());

        orchestrator.handleInbound(fixture.respuestaTopic(), FiscalizacionTestData.resultError());

        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isFalse();
        assertThat(printerRepository.findAll().stream()
                .noneMatch(p -> fixture.ptrReg().equalsIgnoreCase(p.getFiscalSerial()))).isTrue();
        assertThat(sealRepository.findById(fixture.seal().getId()).orElseThrow().getStatus())
                .isEqualTo(SealStatus.DISPONIBLE);
    }

    @Test
    void timeoutAbortsWithoutCreatingPrinter() throws InterruptedException {
        var fixture = FiscalizacionTestData.seed(
                modelRepository, sealRepository, "GRA0000107", "AA:BB:CC:DD:EE:F7", "G1B0107");

        orchestrator.handleInbound(fixture.cmdServerTopic(), FiscalizacionTestData.ptrFiscalizar(
                fixture.ptrReg(), fixture.colonMac(), fixture.precintoNro(), "Azul", "1.1.0", "AEG-R1"));
        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isTrue();

        Thread.sleep(2_500L);

        assertThat(sessionRegistry.hasActiveSession(fixture.compactMac())).isFalse();
        assertThat(printerRepository.findAll().stream()
                .noneMatch(p -> fixture.ptrReg().equalsIgnoreCase(p.getFiscalSerial()))).isTrue();
        verify(mqttService, atLeastOnce()).publish(eq(fixture.comandoTopic()), org.mockito.ArgumentMatchers.anyString());
    }
}
