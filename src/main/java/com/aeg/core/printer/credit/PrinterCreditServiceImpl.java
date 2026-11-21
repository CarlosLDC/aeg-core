package com.aeg.core.printer.credit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.commercial.CommercialPaymentServiceImpl;
import com.aeg.core.commercial.FacturaComercial;
import com.aeg.core.commercial.PagoComercial;
import com.aeg.core.commercial.PagoComercialRepository;
import com.aeg.core.commercial.dto.CommercialPaymentResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.printer.credit.dto.PrinterCreditPaymentRequest;
import com.aeg.core.printer.credit.dto.PrinterCreditPaymentResponse;
import com.aeg.core.printer.credit.dto.PrinterCreditPlanRequest;
import com.aeg.core.printer.credit.dto.PrinterCreditSummaryResponse;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class PrinterCreditServiceImpl implements PrinterCreditService {

    private final PrinterRepository printerRepository;
    private final PrinterCreditPaymentRepository paymentRepository;
    private final SecurityScopeService securityScope;
    private final PagoComercialRepository pagoComercialRepository;

    public PrinterCreditServiceImpl(
            PrinterRepository printerRepository,
            PrinterCreditPaymentRepository paymentRepository,
            SecurityScopeService securityScope) {
        this(printerRepository, paymentRepository, securityScope, null);
    }

    @Autowired
    public PrinterCreditServiceImpl(
            PrinterRepository printerRepository,
            PrinterCreditPaymentRepository paymentRepository,
            SecurityScopeService securityScope,
            PagoComercialRepository pagoComercialRepository) {
        this.printerRepository = printerRepository;
        this.paymentRepository = paymentRepository;
        this.securityScope = securityScope;
        this.pagoComercialRepository = pagoComercialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PrinterCreditSummaryResponse getCreditSummary(Long printerId) {
        Printer printer = findPrinter(printerId);
        securityScope.assertPrinterInScope(printer);
        return buildSummary(printer);
    }

    @Override
    public PrinterCreditSummaryResponse updateCreditPlan(Long printerId, PrinterCreditPlanRequest request) {
        assertAdmin();
        Printer printer = findPrinter(printerId);
        if (printer.getStatus() != PrinterStatus.A_CREDITO
                && printer.getStatus() != PrinterStatus.EN_CONSIGNACION
                && printer.getStatus() != PrinterStatus.ENAJENADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden configurar cuotas en impresoras a crédito, en consignación o enajenadas.");
        }
        printer.setCreditInstallments(request.creditInstallments());
        if (request.finalSalePrice() != null) {
            printer.setFinalSalePrice(request.finalSalePrice());
        }
        printerRepository.save(printer);
        return buildSummary(printer);
    }

    @Override
    public PrinterCreditSummaryResponse registerPayment(Long printerId, PrinterCreditPaymentRequest request) {
        User admin = assertAdmin();
        Printer printer = findPrinter(printerId);
        if (printer.getStatus() != PrinterStatus.A_CREDITO
                && printer.getStatus() != PrinterStatus.ENAJENADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden registrar pagos de cuotas en impresoras con estatus A crédito o Enajenada.");
        }

        PrinterCreditPayment payment = new PrinterCreditPayment();
        payment.setPrinter(printer);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setReference(trimToNull(request.reference()));
        payment.setNotes(trimToNull(request.notes()));
        payment.setRegisteredBy(admin);
        paymentRepository.save(payment);

        if (Boolean.TRUE.equals(request.markAsPaid()) && printer.getStatus() == PrinterStatus.A_CREDITO) {
            printer.setStatus(PrinterStatus.ENAJENADA);
            printer.setPaid(true);
            printerRepository.save(printer);
        }

        return buildSummary(printer);
    }

    @Override
    public PrinterCreditSummaryResponse deletePayment(Long printerId, Long paymentId) {
        assertAdmin();
        Printer printer = findPrinter(printerId);
        PrinterCreditPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit payment not found with id: " + paymentId));
        if (payment.getPrinter() == null || !printer.getId().equals(payment.getPrinter().getId())) {
            throw new ResourceNotFoundException("Credit payment not found for printer: " + printerId);
        }
        paymentRepository.delete(payment);
        paymentRepository.flush();
        return buildSummary(printer);
    }

    private PrinterCreditSummaryResponse buildSummary(Printer printer) {
        List<PrinterCreditPayment> legacyPayments = paymentRepository
                .findByPrinter_IdOrderByPaymentDateDescIdDesc(printer.getId());
        List<PrinterCreditPaymentResponse> paymentDtos = legacyPayments.stream()
                .map(p -> new PrinterCreditPaymentResponse(
                        p.getId(),
                        printer.getId(),
                        p.getAmount(),
                        p.getPaymentDate(),
                        p.getReference(),
                        p.getNotes(),
                        p.getRegisteredBy() != null ? p.getRegisteredBy().getId() : null,
                        p.getRegisteredBy() != null ? p.getRegisteredBy().getName() : null,
                        p.getCreatedAt()))
                .toList();

        FacturaComercial factura = printer.getFacturaComercial();
        if (factura != null && pagoComercialRepository != null) {
            List<PagoComercial> commercialEntities = pagoComercialRepository
                    .findByFacturaComercial_IdOrderByCreatedAtDesc(factura.getId());
            List<CommercialPaymentResponse> commercialPayments = commercialEntities.stream()
                    .map(CommercialPaymentServiceImpl::toResponse)
                    .toList();
            BigDecimal totalPaid = commercialEntities.stream()
                    .map(PagoComercial::getMonto)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = printer.getFinalSalePrice() != null
                    ? printer.getFinalSalePrice().subtract(totalPaid).max(BigDecimal.ZERO)
                    : null;
            long invoicePrinterCount = printerRepository.countByFacturaComercial_Id(factura.getId());

            return new PrinterCreditSummaryResponse(
                    printer.getId(),
                    printer.getFiscalSerial(),
                    printer.getStatus(),
                    printer.getFinalSalePrice(),
                    printer.getCreditInstallments(),
                    totalPaid,
                    remaining,
                    commercialPayments.size(),
                    Boolean.TRUE.equals(printer.getPaid()),
                    paymentDtos,
                    factura.getId(),
                    factura.getNumeroFactura(),
                    factura.getModalidad(),
                    factura.getEstadoCobro(),
                    invoicePrinterCount,
                    commercialPayments);
        }

        BigDecimal totalPaid = legacyPayments.stream()
                .map(PrinterCreditPayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = null;
        if (printer.getFinalSalePrice() != null) {
            remaining = printer.getFinalSalePrice().subtract(totalPaid).max(BigDecimal.ZERO);
        }

        return new PrinterCreditSummaryResponse(
                printer.getId(),
                printer.getFiscalSerial(),
                printer.getStatus(),
                printer.getFinalSalePrice(),
                printer.getCreditInstallments(),
                totalPaid,
                remaining,
                legacyPayments.size(),
                Boolean.TRUE.equals(printer.getPaid()),
                paymentDtos,
                factura != null ? factura.getId() : null,
                factura != null ? factura.getNumeroFactura() : null,
                factura != null ? factura.getModalidad() : null,
                factura != null ? factura.getEstadoCobro() : null,
                factura != null ? printerRepository.countByFacturaComercial_Id(factura.getId()) : 0L,
                List.of());
    }

    private Printer findPrinter(Long printerId) {
        return printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
    }

    private User assertAdmin() {
        User user = securityScope.currentUser();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede gestionar cuotas y pagos a crédito.");
        }
        return user;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
