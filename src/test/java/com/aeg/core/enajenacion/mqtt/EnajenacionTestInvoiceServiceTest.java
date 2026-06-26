package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityRecorder;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityResult;
import com.aeg.core.fiscal.FiscalTicketLatin2;
import com.aeg.core.client.Client;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.EnajenacionTestInvoiceResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class EnajenacionTestInvoiceServiceTest {

    private static final String MAC = "20:6E:F1:88:4C:68";
    private static final String COMPACT_MAC = "206EF1884C68";

    @Mock
    private PrinterRepository printerRepository;

    @Mock
    private MqttService mqttService;

    @Mock
    private EnajenacionActivityRecorder activityRecorder;

    private EnajenacionTestInvoiceService service;

    @BeforeEach
    void setUp() {
        EnajenacionPayloadBuilder payloadBuilder = new EnajenacionPayloadBuilder(new ObjectMapper(), "");
        service = new EnajenacionTestInvoiceService(
                printerRepository,
                payloadBuilder,
                mqttService,
                activityRecorder);
    }

    @Test
    void rejectsNonEnajenadaPrinter() {
        Printer printer = printer(PrinterStatus.ASIGNADA);
        when(printerRepository.findById(1L)).thenReturn(Optional.of(printer));

        assertThatThrownBy(() -> service.sendTestInvoice(1L, "Producto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enajenadas");
    }

    @Test
    void rejectsMissingPrinter() {
        when(printerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendTestInvoice(99L, "Producto"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publishesInvoiceToFiscalComandoTopicWithLatin2ReadyPayload() {
        Printer printer = printer(PrinterStatus.ENAJENADA);
        when(printerRepository.findById(1L)).thenReturn(Optional.of(printer));

        EnajenacionTestInvoiceResponse response = service.sendTestInvoice(1L, "Información técnica");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(topicCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(FiscalMqttTopics.comandoTopic(COMPACT_MAC));
        assertThat(payloadCaptor.getValue()).contains("Información técnica");
        assertThat(payloadCaptor.getValue().getBytes(FiscalTicketLatin2.CHARSET))
                .isNotEqualTo(payloadCaptor.getValue().getBytes(Charset.forName("UTF-8")));

        assertThat(response.topic()).isEqualTo(FiscalMqttTopics.comandoTopic(COMPACT_MAC));
        assertThat(response.fiscalSerial()).isEqualTo("GRA0000017");
        assertThat(response.mac()).isEqualTo(MAC);
        assertThat(response.payload()).isEqualTo(payloadCaptor.getValue());

        verify(activityRecorder).recordAdminOutbound(
                eq(FiscalMqttTopics.comandoTopic(COMPACT_MAC)),
                any(String.class),
                eq(COMPACT_MAC),
                eq(1L),
                eq("GRA0000017"),
                eq(EnajenacionActivityResult.PUBLISHED),
                eq("Admin test invoice"));
    }

    @Test
    void stillReturnsResponseWhenActivityLogFails() {
        Printer printer = printer(PrinterStatus.ENAJENADA);
        when(printerRepository.findById(1L)).thenReturn(Optional.of(printer));
        org.mockito.Mockito.doThrow(new RuntimeException("relation does not exist"))
                .when(activityRecorder)
                .recordAdminOutbound(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());

        EnajenacionTestInvoiceResponse response = service.sendTestInvoice(1L, "Producto");

        assertThat(response.topic()).isEqualTo(FiscalMqttTopics.comandoTopic(COMPACT_MAC));
        verify(mqttService).publish(any(), any());
    }

    private static Printer printer(PrinterStatus status) {
        Client client = new Client();
        client.setId(10L);
        Printer printer = new Printer();
        printer.setId(1L);
        printer.setStatus(status);
        printer.setFiscalSerial("GRA0000017");
        printer.setMacAddress(MAC);
        printer.setClient(client);
        return printer;
    }
}
