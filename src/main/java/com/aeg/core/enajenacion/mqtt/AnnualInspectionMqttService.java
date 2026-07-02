package com.aeg.core.enajenacion.mqtt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.AnnualInspectionStaInfResponse;
import com.aeg.core.mqtt.dto.AnnualInspectionSubmitResponse;
import com.aeg.core.mqtt.dto.AnnualInspectionTestCreditNoteResponse;
import com.aeg.core.mqtt.dto.AnnualInspectionTestInvoiceResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.servicecenter.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnnualInspectionMqttService {

    private static final int STA_INF_TIMEOUT_SECONDS = 1;
    private static final String PRINTER_QUERY_TIMEOUT_MESSAGE =
            "La impresora no respondió a tiempo. Verifique que esté encendida y conectada a la red, e intente nuevamente.";
    private static final String PRINTER_QUERY_ERROR_MESSAGE =
            "No se pudo consultar la impresora. Verifique que esté encendida y conectada a la red, e intente nuevamente.";

    private final PrinterRepository printerRepository;
    private final EnajenacionPayloadBuilder payloadBuilder;
    private final MqttService mqttService;
    private final FiscalMqttSyncResponseAwaiter syncResponseAwaiter;
    private final FiscalResponseValidator responseValidator;
    private final EnajenacionMqttSettings settings;

    public AnnualInspectionMqttService(
            PrinterRepository printerRepository,
            EnajenacionPayloadBuilder payloadBuilder,
            MqttService mqttService,
            FiscalMqttSyncResponseAwaiter syncResponseAwaiter,
            FiscalResponseValidator responseValidator,
            EnajenacionMqttSettings settings) {
        this.printerRepository = printerRepository;
        this.payloadBuilder = payloadBuilder;
        this.mqttService = mqttService;
        this.syncResponseAwaiter = syncResponseAwaiter;
        this.responseValidator = responseValidator;
        this.settings = settings;
    }

    public AnnualInspectionStaInfResponse requestRegistrationStatus(Long printerId) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        assertPrinterEligible(printer);

        String compactMac = MacAddressNormalizer.requireCompactForm(printer.getMacAddress());
        String topic = FiscalMqttTopics.comandoTopic(compactMac);
        String commandPayload = payloadBuilder.buildRegistrationStatusPayload();
        int timeoutSeconds = STA_INF_TIMEOUT_SECONDS;

        CompletableFuture<FiscalMqttResponseItem> responseFuture = syncResponseAwaiter.register(
                compactMac,
                EnajenacionConstants.CMD_STA_INF);
        OffsetDateTime publishedAt = OffsetDateTime.now();

        try {
            mqttService.publish(topic, commandPayload);
            FiscalMqttResponseItem response = responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            String registroImpresora = responseValidator.validateStaInfRegistrationResponse(response);
            log.info(
                    "Annual inspection StaInf ok printerId={} mac={} registro={}",
                    printerId,
                    compactMac,
                    registroImpresora);
            return new AnnualInspectionStaInfResponse(
                    registroImpresora,
                    topic,
                    printer.getFiscalSerial(),
                    printer.getMacAddress(),
                    commandPayload,
                    response,
                    publishedAt);
        } catch (TimeoutException ex) {
            throw new EnajenacionProtocolException(PRINTER_QUERY_TIMEOUT_MESSAGE);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new EnajenacionProtocolException(PRINTER_QUERY_ERROR_MESSAGE);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException(PRINTER_QUERY_ERROR_MESSAGE);
        } finally {
            syncResponseAwaiter.cancel(compactMac);
        }
    }

    public AnnualInspectionTestInvoiceResponse sendTestInvoice(Long printerId, String productDescription) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        assertPrinterEligible(printer);

        String compactMac = MacAddressNormalizer.requireCompactForm(printer.getMacAddress());
        String topic = FiscalMqttTopics.comandoTopic(compactMac);
        String commandPayload = payloadBuilder.buildAnnualInspectionTestInvoicePayload(productDescription);
        int timeoutSeconds = settings.invoiceTimeoutSeconds();

        CompletableFuture<List<FiscalMqttResponseItem>> responseFuture = syncResponseAwaiter.registerArrayTerminal(
                compactMac,
                EnajenacionConstants.CMD_END_FAC);
        OffsetDateTime publishedAt = OffsetDateTime.now();

        try {
            mqttService.publish(topic, commandPayload);
            List<FiscalMqttResponseItem> response = responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            int numeroFacturaPrueba = responseValidator.validateAnnualInspectionTestInvoiceResponse(response);
            log.info(
                    "Annual inspection test invoice ok printerId={} mac={} numeroFactura={}",
                    printerId,
                    compactMac,
                    numeroFacturaPrueba);
            return new AnnualInspectionTestInvoiceResponse(
                    numeroFacturaPrueba,
                    topic,
                    printer.getFiscalSerial(),
                    printer.getMacAddress(),
                    commandPayload,
                    response,
                    publishedAt);
        } catch (TimeoutException ex) {
            throw new EnajenacionProtocolException(
                    "Tiempo de espera agotado esperando respuesta de factura de prueba.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new EnajenacionProtocolException(
                    "Error esperando respuesta de factura de prueba: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException("Factura de prueba interrumpida.");
        } finally {
            syncResponseAwaiter.cancel(compactMac);
        }
    }

    public AnnualInspectionTestCreditNoteResponse sendTestCreditNote(
            Long printerId,
            int numeroFacturaPrueba,
            String registroImpresora,
            String productDescription) {
        if (numeroFacturaPrueba <= 0) {
            throw new IllegalArgumentException("numeroFacturaPrueba debe ser mayor que cero.");
        }
        if (registroImpresora == null || registroImpresora.isBlank()) {
            throw new IllegalArgumentException("registroImpresora es requerido.");
        }

        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        assertPrinterEligible(printer);

        String compactMac = MacAddressNormalizer.requireCompactForm(printer.getMacAddress());
        String topic = FiscalMqttTopics.comandoTopic(compactMac);
        String commandPayload = payloadBuilder.buildAnnualInspectionTestCreditNotePayload(
                numeroFacturaPrueba,
                registroImpresora,
                productDescription);
        int timeoutSeconds = settings.creditNoteTimeoutSeconds();

        CompletableFuture<List<FiscalMqttResponseItem>> responseFuture = syncResponseAwaiter.registerArrayTerminal(
                compactMac,
                EnajenacionConstants.CMD_END_NC);
        OffsetDateTime publishedAt = OffsetDateTime.now();

        try {
            mqttService.publish(topic, commandPayload);
            List<FiscalMqttResponseItem> response = responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            responseValidator.validateAnnualInspectionTestCreditNoteResponse(response);
            log.info(
                    "Annual inspection test credit note ok printerId={} mac={} numeroFactura={} registro={}",
                    printerId,
                    compactMac,
                    numeroFacturaPrueba,
                    registroImpresora.trim());
            return new AnnualInspectionTestCreditNoteResponse(
                    topic,
                    printer.getFiscalSerial(),
                    printer.getMacAddress(),
                    commandPayload,
                    response,
                    publishedAt);
        } catch (TimeoutException ex) {
            throw new EnajenacionProtocolException(
                    "Tiempo de espera agotado esperando respuesta de nota de crédito de prueba.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new EnajenacionProtocolException(
                    "Error esperando respuesta de nota de crédito de prueba: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException("Nota de crédito de prueba interrumpida.");
        } finally {
            syncResponseAwaiter.cancel(compactMac);
        }
    }

    public AnnualInspectionSubmitResponse submitInspection(
            Long printerId,
            boolean chkPrecinto,
            boolean chkEtiquetaFiscal,
            boolean chkFactura,
            boolean chkNotaCredito,
            boolean chkSensorPapel) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        assertPrinterEligible(printer);

        AnnualInspectionInspAo inspAo = AnnualInspectionInspAo.fromChecklist(
                chkPrecinto,
                chkEtiquetaFiscal,
                chkFactura,
                chkNotaCredito,
                chkSensorPapel);
        long dataTimestamp = VenezuelaNaiveUnixTimestamp.currentSeconds();
        String compactMac = MacAddressNormalizer.requireCompactForm(printer.getMacAddress());
        String topic = FiscalMqttTopics.comandoTopic(compactMac);
        String commandPayload = payloadBuilder.buildSetDateRevOPayload(dataTimestamp, inspAo);
        int timeoutSeconds = settings.regStatusTimeoutSeconds();

        CompletableFuture<FiscalMqttResponseItem> responseFuture = syncResponseAwaiter.register(
                compactMac,
                EnajenacionConstants.CMD_SET_DATE_REV_O);
        OffsetDateTime publishedAt = OffsetDateTime.now();

        try {
            mqttService.publish(topic, commandPayload);
            FiscalMqttResponseItem response = responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            responseValidator.validateSetDateRevOResponse(response);
            log.info(
                    "Annual inspection SetDateRevO ok printerId={} mac={} timestamp={}",
                    printerId,
                    compactMac,
                    dataTimestamp);
            return new AnnualInspectionSubmitResponse(
                    dataTimestamp,
                    inspAo,
                    topic,
                    printer.getFiscalSerial(),
                    printer.getMacAddress(),
                    commandPayload,
                    response,
                    publishedAt);
        } catch (TimeoutException ex) {
            throw new EnajenacionProtocolException(
                    "Tiempo de espera agotado esperando respuesta SetDateRevO de la impresora.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new EnajenacionProtocolException(
                    "Error esperando respuesta SetDateRevO: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException("Envío SetDateRevO interrumpido.");
        } finally {
            syncResponseAwaiter.cancel(compactMac);
        }
    }

    private static void assertPrinterEligible(Printer printer) {
        if (printer.getStatus() != PrinterStatus.ENAJENADA) {
            throw new IllegalArgumentException(
                    "La inspección anual MQTT solo aplica a impresoras enajenadas.");
        }
        if (printer.getClientId() == null) {
            throw new IllegalArgumentException("La impresora no tiene cliente asignado.");
        }
        if (printer.getFiscalSerial() == null || printer.getFiscalSerial().isBlank()) {
            throw new IllegalArgumentException("La impresora no tiene serial fiscal.");
        }
        if (printer.getMacAddress() == null || printer.getMacAddress().isBlank()) {
            throw new IllegalArgumentException(
                    "La impresora no tiene dirección MAC registrada. Sin la MAC no es posible comunicarse con la impresora para la inspección anual.");
        }
    }
}
