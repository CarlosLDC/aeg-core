package com.aeg.core.commercial;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.commercial.dto.CommercialPaymentRequest;
import com.aeg.core.commercial.dto.CommercialPaymentResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class CommercialPaymentServiceImpl implements CommercialPaymentService {

    private static final Set<String> ALLOWED_METODOS_BANCO = Set.of(
            "BNC", "BINANCE", "ZELLE", "DOLARES_EFECTIVO", "BCV", "OTRO");

    private final PagoComercialRepository pagoRepository;
    private final FacturaComercialRepository facturaRepository;

    public CommercialPaymentServiceImpl(
            PagoComercialRepository pagoRepository,
            FacturaComercialRepository facturaRepository) {
        this.pagoRepository = pagoRepository;
        this.facturaRepository = facturaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommercialPaymentResponse> findAll(Long facturaComercialId) {
        List<PagoComercial> payments = facturaComercialId != null
                ? pagoRepository.findByFacturaComercial_IdOrderByCreatedAtDesc(facturaComercialId)
                : pagoRepository.findAllByOrderByCreatedAtDesc();
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
        payment.setCreatedAt(OffsetDateTime.now());
        PagoComercial saved = pagoRepository.save(payment);
        return toResponse(saved);
    }

    @Override
    public CommercialPaymentResponse update(Long id, CommercialPaymentRequest request) {
        PagoComercial payment = findEntityById(id);
        applyRequest(payment, request);
        PagoComercial saved = pagoRepository.save(payment);
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        PagoComercial payment = findEntityById(id);
        pagoRepository.delete(payment);
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

    private static CommercialPaymentResponse toResponse(PagoComercial payment) {
        FacturaComercial factura = payment.getFacturaComercial();
        ClienteComercial cliente = factura != null ? factura.getClienteComercial() : null;
        return CommercialPaymentResponse.builder()
                .id(payment.getId())
                .facturaComercialId(factura != null ? factura.getId() : null)
                .numeroFactura(factura != null ? factura.getNumeroFactura() : null)
                .clienteComercialId(cliente != null ? cliente.getId() : null)
                .clienteComercialNombre(cliente != null ? cliente.getNombre() : null)
                .metodoBanco(payment.getMetodoBanco())
                .referencia(payment.getReferencia())
                .monto(payment.getMonto())
                .cuentaDestino(payment.getCuentaDestino())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
