package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.EnajenacionVerifyStatusResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.SecurityScopeService;

@ExtendWith(MockitoExtension.class)
class EnajenacionVerifyStatusServiceTest {

    private static final String MAC = "206EF1884C68";
    private static final String FISCAL_SERIAL = "GRA00000518";

    @Mock
    private PrinterRepository printerRepository;

    @Mock
    private SecurityScopeService securityScope;

    @Mock
    private EnajenacionPayloadBuilder payloadBuilder;

    @Mock
    private MqttService mqttService;

    @Mock
    private FiscalMqttSyncResponseAwaiter syncResponseAwaiter;

    @Mock
    private FiscalResponseValidator responseValidator;

    @Mock
    private EnajenacionCompletionService completionService;

    private EnajenacionVerifyStatusService service;

    @BeforeEach
    void setUp() {
        service = new EnajenacionVerifyStatusService(
                printerRepository,
                securityScope,
                payloadBuilder,
                mqttService,
                syncResponseAwaiter,
                responseValidator,
                completionService);
    }

    @Test
    void returnsEarlyIfAlreadyEnajenadaInDb() {
        Printer printer = new Printer();
        printer.setId(10L);
        printer.setFiscalSerial(FISCAL_SERIAL);
        printer.setStatus(PrinterStatus.ENAJENADA);

        when(printerRepository.findById(10L)).thenReturn(Optional.of(printer));

        EnajenacionVerifyStatusResponse response = service.verifyAndSyncStatus(10L, true);

        assertThat(response.alreadyEnajenada()).isTrue();
        assertThat(response.synced()).isFalse();
        assertThat(response.match()).isTrue();
    }

    @Test
    void verifiesAndSyncsWhenPrinterReportsMatchingFiscalSerial() {
        Printer printer = new Printer();
        printer.setId(10L);
        printer.setFiscalSerial(FISCAL_SERIAL);
        printer.setMacAddress(MAC);
        printer.setStatus(PrinterStatus.ASIGNADA);

        when(printerRepository.findById(10L)).thenReturn(Optional.of(printer));
        when(payloadBuilder.buildRegistrationStatusPayload()).thenReturn("{\"cmd\":\"StaInf\"}");

        FiscalMqttResponseItem mockResponse = new FiscalMqttResponseItem("StaInf", 0, null, FISCAL_SERIAL);
        when(syncResponseAwaiter.register(eq(MAC), eq("StaInf")))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));
        when(responseValidator.validateStaInfRegistrationResponse(mockResponse)).thenReturn(FISCAL_SERIAL);

        EnajenacionVerifyStatusResponse response = service.verifyAndSyncStatus(10L, true);

        assertThat(response.alreadyEnajenada()).isTrue();
        assertThat(response.synced()).isTrue();
        assertThat(response.match()).isTrue();
        assertThat(response.reportedFiscalSerial()).isEqualTo(FISCAL_SERIAL);
        verify(completionService).markEnajenada(10L);
    }

    @Test
    void detectsMismatchWhenPrinterReportsDifferentSerial() {
        Printer printer = new Printer();
        printer.setId(10L);
        printer.setFiscalSerial(FISCAL_SERIAL);
        printer.setMacAddress(MAC);
        printer.setStatus(PrinterStatus.ASIGNADA);

        when(printerRepository.findById(10L)).thenReturn(Optional.of(printer));
        when(payloadBuilder.buildRegistrationStatusPayload()).thenReturn("{\"cmd\":\"StaInf\"}");

        FiscalMqttResponseItem mockResponse = new FiscalMqttResponseItem("StaInf", 0, null, "GRA9999999");
        when(syncResponseAwaiter.register(eq(MAC), eq("StaInf")))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));
        when(responseValidator.validateStaInfRegistrationResponse(mockResponse)).thenReturn("GRA9999999");

        EnajenacionVerifyStatusResponse response = service.verifyAndSyncStatus(10L, true);

        assertThat(response.match()).isFalse();
        assertThat(response.alreadyEnajenada()).isTrue();
        assertThat(response.synced()).isFalse();
        assertThat(response.reportedFiscalSerial()).isEqualTo("GRA9999999");
    }

    @Test
    void handlesPrinterWithoutActiveRegistration() {
        Printer printer = new Printer();
        printer.setId(10L);
        printer.setFiscalSerial(FISCAL_SERIAL);
        printer.setMacAddress(MAC);
        printer.setStatus(PrinterStatus.ASIGNADA);

        when(printerRepository.findById(10L)).thenReturn(Optional.of(printer));
        when(payloadBuilder.buildRegistrationStatusPayload()).thenReturn("{\"cmd\":\"StaInf\"}");

        FiscalMqttResponseItem mockResponse = new FiscalMqttResponseItem("StaInf", 1, null, null);
        when(syncResponseAwaiter.register(eq(MAC), eq("StaInf")))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));
        when(responseValidator.validateStaInfRegistrationResponse(mockResponse))
                .thenThrow(new EnajenacionProtocolException("No registration"));

        EnajenacionVerifyStatusResponse response = service.verifyAndSyncStatus(10L, true);

        assertThat(response.match()).isFalse();
        assertThat(response.alreadyEnajenada()).isFalse();
        assertThat(response.synced()).isFalse();
    }
}
