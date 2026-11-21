package com.aeg.core.commercial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.commercial.dto.CommercialPaymentRequest;
import com.aeg.core.commercial.dto.CommercialPaymentResponse;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class CommercialPaymentServiceImpl implements CommercialPaymentService {

    private static final Set<String> ALLOWED_METODOS_BANCO = Set.of(
            "BNC", "BINANCE", "ZELLE", "DOLARES_EFECTIVO", "BCV", "OTRO");

    private final PagoComercialRepository pagoRepository;
    private final FacturaComercialRepository facturaRepository;
    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;

    public CommercialPaymentServiceImpl(
            PagoComercialRepository pagoRepository,
            FacturaComercialRepository facturaRepository) {
        this(pagoRepository, facturaRepository, null, null);
    }

    public CommercialPaymentServiceImpl(
            PagoComercialRepository pagoRepository,
            FacturaComercialRepository facturaRepository,
            PrinterRepository printerRepository) {
        this(pagoRepository, facturaRepository, printerRepository, null);
    }

    @Autowired
    public CommercialPaymentServiceImpl(
            PagoComercialRepository pagoRepository,
            FacturaComercialRepository facturaRepository,
            PrinterRepository printerRepository,
            SecurityScopeService securityScope) {
        this.pagoRepository = pagoRepository;
        this.facturaRepository = facturaRepository;
        this.printerRepository = printerRepository;
        this.securityScope = securityScope;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommercialPaymentResponse> findAll(Long facturaComercialId) {
        List<PagoComercial> payments = facturaComercialId != null
                ? pagoRepository.findByFacturaComercial_IdOrderByFechaPagoDescIdDesc(facturaComercialId)
                : pagoRepository.findAllByOrderByFechaPagoDescIdDesc();
        return payments.stream()
                .map(CommercialPaymentServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommercialPaymentResponse findById(Long id) {
        PagoComercial payment = findEntityById(id);
        return toResponse(payment);
    }

    @Override
    public CommercialPaymentResponse create(CommercialPaymentRequest request) {
        PagoComercial payment = new PagoComercial();
        applyRequest(payment, request);
        payment.setRegisteredBy(resolveCurrentUserOrNull());
        payment.setCreatedAt(OffsetDateTime.now());
        PagoComercial saved = pagoRepository.save(payment);
        reconcileInvoiceCollectionStatus(saved.getFacturaComercial(), request, true);
        return toResponse(saved);
    }

    @Override
    public CommercialPaymentResponse update(Long id, CommercialPaymentRequest request) {
        PagoComercial payment = findEntityById(id);
        FacturaComercial previousFactura = payment.getFacturaComercial();
        applyRequest(payment, request);
        PagoComercial saved = pagoRepository.save(payment);
        boolean movedInvoice = previousFactura != null
                && saved.getFacturaComercial() != null
                && !Objects.equals(previousFactura.getId(), saved.getFacturaComercial().getId());
        if (movedInvoice) {
            pagoRepository.flush();
            revertPartialStatusIfNoPaymentsRemain(previousFactura);
        }
        reconcileInvoiceCollectionStatus(saved.getFacturaComercial(), request, movedInvoice);
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        PagoComercial payment = findEntityById(id);
        FacturaComercial factura = payment.getFacturaComercial();
        pagoRepository.delete(payment);
        pagoRepository.flush();
        revertPartialStatusIfNoPaymentsRemain(factura);
    }

    private void revertPartialStatusIfNoPaymentsRemain(FacturaComercial factura) {
        if (factura == null || factura.getId() == null) {
            return;
        }
        if ("PAGO_PARCIAL".equalsIgnoreCase(factura.getEstadoCobro())
                && pagoRepository.countByFacturaComercial_Id(factura.getId()) == 0) {
            factura.setEstadoCobro("PENDIENTE_PAGO");
            facturaRepository.save(factura);
        }
    }

    private void reconcileInvoiceCollectionStatus(
            FacturaComercial factura,
            CommercialPaymentRequest request,
            boolean shouldPromotePending) {
        if (factura == null) {
            return;
        }
        if (Boolean.TRUE.equals(request.getMarkInvoiceAsPaid())) {
            factura.setEstadoCobro("PAGADO");
            FacturaComercial savedFactura = facturaRepository.save(factura);
            CommercialInvoiceServiceImpl.settleCreditPrinters(savedFactura, printerRepository);
            return;
        }
        if (shouldPromotePending
                && "PENDIENTE_PAGO".equalsIgnoreCase(factura.getEstadoCobro())
                && request.getMonto() != null
                && request.getMonto().compareTo(BigDecimal.ZERO) > 0) {
            factura.setEstadoCobro("PAGO_PARCIAL");
            facturaRepository.save(factura);
        }
    }

    private User resolveCurrentUserOrNull() {
        if (securityScope == null) {
            return null;
        }
        try {
            return securityScope.currentUser();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private PagoComercial findEntityById(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial payment not found with id: " + id));
    }

    private FacturaComercial findFacturaById(Long facturaId) {
        return facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial invoice not found with id: " + facturaId));
    }

    private void applyRequest(PagoComercial target, CommercialPaymentRequest request) {
        FacturaComercial factura = findFacturaById(request.getFacturaComercialId());
        String metodoBanco = normalizeMetodoBanco(request.getMetodoBanco());

        target.setFacturaComercial(factura);
        target.setMetodoBanco(metodoBanco);
        target.setReferencia(trimToNull(request.getReferencia()));
        target.setMonto(request.getMonto());
        target.setCuentaDestino(trimToNull(request.getCuentaDestino()));
        LocalDate effectiveDate = request.getFechaPago() != null
                ? request.getFechaPago()
                : (target.getFechaPago() != null ? target.getFechaPago() : LocalDate.now());
        target.setFechaPago(effectiveDate);
        target.setNotas(trimToNull(request.getNotas()));
    }

    private static String normalizeMetodoBanco(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo metodoBanco es obligatorio.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_METODOS_BANCO.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valor inválido para metodoBanco: " + value);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static CommercialPaymentResponse toResponse(PagoComercial payment) {
        FacturaComercial factura = payment.getFacturaComercial();
        ClienteComercial cliente = factura != null ? factura.getClienteComercial() : null;
        User registeredBy = payment.getRegisteredBy();
        return CommercialPaymentResponse.builder()
                .id(payment.getId())
                .facturaComercialId(factura != null ? factura.getId() : null)
                .numeroFactura(factura != null ? factura.getNumeroFactura() : null)
                .empresaId(factura != null && factura.getCompany() != null ? factura.getCompany().getId() : null)
                .clienteComercialId(cliente != null ? cliente.getId() : null)
                .clienteComercialNombre(CommercialInvoiceServiceImpl.resolveRecipientDisplayName(factura))
                .metodoBanco(payment.getMetodoBanco())
                .referencia(payment.getReferencia())
                .monto(payment.getMonto())
                .cuentaDestino(payment.getCuentaDestino())
                .fechaPago(payment.getFechaPago())
                .notas(payment.getNotas())
                .registeredById(registeredBy != null ? registeredBy.getId() : null)
                .registeredByName(registeredBy != null ? registeredBy.getName() : null)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
