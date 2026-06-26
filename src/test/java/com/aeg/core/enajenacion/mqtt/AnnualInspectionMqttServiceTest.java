package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.client.Client;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.AnnualInspectionStaInfResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AnnualInspectionMqttServiceTest {

    private static final String MAC = "20:6E:F1:88:4C:68";
    private static final String COMPACT_MAC = "206EF1884C68";

    @Mock
    private PrinterRepository printerRepository;

    @Mock
    private MqttService mqttService;

    @Mock
    private FiscalMqttSyncResponseAwaiter syncResponseAwaiter;

    @Mock
    private FiscalResponseValidator responseValidator;

    @Mock
    private EnajenacionMqttSettings settings;

    private AnnualInspectionMqttService service;

    @BeforeEach
    void setUp() {
        EnajenacionPayloadBuilder payloadBuilder = new EnajenacionPayloadBuilder(new ObjectMapper(), "");
        service = new AnnualInspectionMqttService(
                printerRepository,
                payloadBuilder,
                mqttService,
                syncResponseAwaiter,
                responseValidator,
                settings);
    }

    @Test
    void publishesStaInfToFiscalComandoTopicAndWaitsOnRespuesta() {
        Printer printer = enajenadaPrinter();
        when(printerRepository.findById(1L)).thenReturn(Optional.of(printer));
        when(settings.regStatusTimeoutSeconds()).thenReturn(30);

        FiscalMqttResponseItem responseItem = new FiscalMqttResponseItem("StaInf", 0, null, "GRA0000017");
        CompletableFuture<FiscalMqttResponseItem> future = CompletableFuture.completedFuture(responseItem);
        when(syncResponseAwaiter.register(eq(COMPACT_MAC), eq(EnajenacionConstants.CMD_STA_INF)))
                .thenReturn(future);
        when(responseValidator.validateStaInfRegistrationResponse(responseItem)).thenReturn("GRA0000017");

        AnnualInspectionStaInfResponse response = service.requestRegistrationStatus(1L);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(topicCaptor.capture(), any(String.class));

        assertThat(topicCaptor.getValue()).isEqualTo(FiscalMqttTopics.comandoTopic(COMPACT_MAC));
        assertThat(response.topic()).isEqualTo(FiscalMqttTopics.comandoTopic(COMPACT_MAC));
        verify(syncResponseAwaiter).cancel(COMPACT_MAC);
    }

    private static Printer enajenadaPrinter() {
        Client client = new Client();
        client.setId(10L);
        Printer printer = new Printer();
        printer.setId(1L);
        printer.setStatus(PrinterStatus.ENAJENADA);
        printer.setFiscalSerial("GRA0000017");
        printer.setMacAddress(MAC);
        printer.setClient(client);
        return printer;
    }
}
