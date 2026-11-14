package com.aeg.core.commercial;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.branch.Branch;
import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceResponse;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class CommercialInvoiceServiceImpl implements CommercialInvoiceService {

    private static final Set<String> ALLOWED_MODALIDADES = Set.of(
            "VENTA", "CONSIGNACION", "DONACION_REGALO", "PRESTAMO", "MUESTRA_SENIAT");

    private static final Set<String> ALLOWED_ESTADOS_COBRO = Set.of(
            "PAGADO", "PAGO_PARCIAL", "DEBE_IVA", "PENDIENTE_PAGO", "POR_FACTURAR", "SIN_CARGO");

    private final FacturaComercialRepository facturaRepository;
    private final CompanyRepository companyRepository;
    private final PagoComercialRepository pagoRepository;
    private final PrinterRepository printerRepository;

    public CommercialInvoiceServiceImpl(
            FacturaComercialRepository facturaRepository,
            CompanyRepository companyRepository,
            PagoComercialRepository pagoRepository,
            PrinterRepository printerRepository) {
        this.facturaRepository = facturaRepository;
        this.companyRepository = companyRepository;
        this.pagoRepository = pagoRepository;
        this.printerRepository = printerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommercialInvoiceResponse> findAll() {
        List<FacturaComercial> invoices = facturaRepository.findAllByOrderByCreatedAtDesc();
        if (invoices.isEmpty()) {
            return List.of();
        }
        Map<Long, PaymentSummary> paymentSummaries = loadPaymentSummaries();
        Map<Long, Long> printerCounts = loadPrinterCounts();
        return invoices.stream()
                .map(invoice -> {
                    PaymentSummary summary = paymentSummaries.getOrDefault(invoice.getId(), PaymentSummary.EMPTY);
                    long printerCount = printerCounts.getOrDefault(invoice.getId(), 0L);
                    return toResponse(invoice, summary.count(), summary.total(), printerCount);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommercialInvoiceResponse findById(Long id) {
        FacturaComercial invoice = findEntityById(id);
        return buildSingleResponse(invoice);
    }

    @Override
    public CommercialInvoiceResponse create(CommercialInvoiceRequest request) {
        FacturaComercial invoice = new FacturaComercial();
        applyRequest(invoice, request);
        invoice.setCreatedAt(OffsetDateTime.now());
        FacturaComercial saved = facturaRepository.save(invoice);
        return toResponse(saved, 0L, BigDecimal.ZERO, 0L);
    }

    @Override
    public CommercialInvoiceResponse update(Long id, CommercialInvoiceRequest request) {
        FacturaComercial invoice = findEntityById(id);
        applyRequest(invoice, request);
        FacturaComercial saved = facturaRepository.save(invoice);
        return buildSingleResponse(saved);
    }

    @Override
    public void delete(Long id) {
        FacturaComercial invoice = findEntityById(id);
        printerRepository.clearFacturaComercialByFacturaId(invoice.getId());
        facturaRepository.delete(invoice);
    }

    private FacturaComercial findEntityById(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial invoice not found with id: " + id));
    }

    private void applyRequest(FacturaComercial target, CommercialInvoiceRequest request) {
        String numeroFactura = trimToNull(request.getNumeroFactura());
        if (numeroFactura == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de factura es obligatorio.");
        }
        Long empresaId = request.getEmpresaId();
        if (empresaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa es obligatoria.");
        }
        String modalidad = normalizeRequiredEnum(request.getModalidad(), ALLOWED_MODALIDADES, "modalidad");
        String estadoCobro = normalizeRequiredEnum(request.getEstadoCobro(), ALLOWED_ESTADOS_COBRO, "estadoCobro");

        Company company = companyRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + empresaId));

        target.setCompany(company);
        target.setDistributor(null);
        target.setClient(null);
        target.setClienteComercial(null);
        target.setNumeroFactura(numeroFactura);
        target.setModalidad(modalidad);
        target.setEstadoCobro(estadoCobro);
    }

    private CommercialInvoiceResponse buildSingleResponse(FacturaComercial invoice) {
        long paymentCount = pagoRepository.countByFacturaComercial_Id(invoice.getId());
        BigDecimal paidAmountTotal = pagoRepository.findByFacturaComercial_Id(invoice.getId()).stream()
                .map(PagoComercial::getMonto)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long printerCount = printerRepository.countByFacturaComercial_Id(invoice.getId());
        return toResponse(invoice, paymentCount, paidAmountTotal, printerCount);
    }

    private Map<Long, PaymentSummary> loadPaymentSummaries() {
        Map<Long, PaymentSummary> map = new HashMap<>();
        for (Object[] row : pagoRepository.summarizePaymentsByFacturaComercialId()) {
            if (row != null && row.length >= 3 && row[0] instanceof Number invoiceId && row[1] instanceof Number count) {
                BigDecimal total = toBigDecimal(row[2]);
                map.put(invoiceId.longValue(), new PaymentSummary(count.longValue(), total));
            }
        }
        return map;
    }

    private Map<Long, Long> loadPrinterCounts() {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : printerRepository.countPrintersGroupedByFacturaComercialId()) {
            if (row != null && row.length >= 2 && row[0] instanceof Number invoiceId && row[1] instanceof Number count) {
                map.put(invoiceId.longValue(), count.longValue());
            }
        }
        return map;
    }

    private static BigDecimal toBigDecimal(Object raw) {
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static String normalizeRequiredEnum(String value, Set<String> allowed, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + fieldName + " es obligatorio.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valor inválido para " + fieldName + ": " + value);
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

    static String resolveCompanyDisplayName(Company company) {
        if (company == null) {
            return null;
        }
        String businessName = trimToNull(company.getBusinessName());
        if (businessName != null) {
            return businessName;
        }
        return trimToNull(company.getRif());
    }

    static String resolveBranchDisplayName(Branch branch) {
        return branch != null ? resolveCompanyDisplayName(branch.getCompany()) : null;
    }

    static String resolveRecipientDisplayName(FacturaComercial invoice) {
        if (invoice == null) {
            return null;
        }
        if (invoice.getCompany() != null) {
            String name = resolveCompanyDisplayName(invoice.getCompany());
            if (name != null) {
                return name;
            }
        }
        if (invoice.getDistributor() != null) {
            String name = resolveBranchDisplayName(invoice.getDistributor().getBranch());
            if (name != null) {
                return name;
            }
        }
        if (invoice.getClient() != null) {
            String name = resolveBranchDisplayName(invoice.getClient().getBranch());
            if (name != null) {
                return name;
            }
        }
        ClienteComercial legacy = invoice.getClienteComercial();
        return legacy != null ? legacy.getNombre() : null;
    }

    private static CommercialInvoiceResponse toResponse(
            FacturaComercial invoice,
            long paymentCount,
            BigDecimal paidAmountTotal,
            long printerCount) {
        Company company = invoice.getCompany();
        ClienteComercial legacy = invoice.getClienteComercial();
        String companyName = company != null ? resolveCompanyDisplayName(company) : null;
        String companyRif = company != null ? trimToNull(company.getRif()) : null;
        String recipientName = resolveRecipientDisplayName(invoice);
        return CommercialInvoiceResponse.builder()
                .id(invoice.getId())
                .numeroFactura(invoice.getNumeroFactura())
                .empresaId(company != null ? company.getId() : null)
                .empresaNombre(companyName)
                .empresaRif(companyRif)
                .clienteComercialId(legacy != null ? legacy.getId() : null)
                .clienteComercialNombre(recipientName)
                .modalidad(invoice.getModalidad())
                .estadoCobro(invoice.getEstadoCobro())
                .createdAt(invoice.getCreatedAt())
                .paymentCount(paymentCount)
                .paidAmountTotal(paidAmountTotal != null ? paidAmountTotal : BigDecimal.ZERO)
                .printerCount(printerCount)
                .build();
    }

    private record PaymentSummary(long count, BigDecimal total) {
        private static final PaymentSummary EMPTY = new PaymentSummary(0L, BigDecimal.ZERO);
    }
}
