package com.aeg.core.commercial;

import java.util.List;

import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceResponse;

public interface CommercialInvoiceService {

    List<CommercialInvoiceResponse> findAll();

    CommercialInvoiceResponse findById(Long id);

    CommercialInvoiceResponse create(CommercialInvoiceRequest request);

    CommercialInvoiceResponse update(Long id, CommercialInvoiceRequest request);

    void delete(Long id);
}
