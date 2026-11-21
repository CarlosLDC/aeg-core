package com.aeg.core.commercial;

import java.util.List;

import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceResponse;

public interface CommercialInvoiceService {

    List<CommercialInvoiceResponse> findAll();

    CommercialInvoiceResponse findById(Long id);

    FacturaComercial createEntity(CommercialInvoiceRequest request);

    CommercialInvoiceResponse create(CommercialInvoiceRequest request);

    CommercialInvoiceResponse update(Long id, CommercialInvoiceRequest request);

    CommercialInvoiceResponse markAsPaid(Long id);

    int settleCreditPrintersIfPaid(FacturaComercial invoice);

    void delete(Long id);

    CommercialInvoiceResponse syncPrinters(Long id, List<Long> printerIds);

    CommercialInvoiceResponse unlinkPrinter(Long id, Long printerId);
}
