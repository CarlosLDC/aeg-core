package com.aeg.core.commercial;

import java.util.List;

import com.aeg.core.commercial.dto.CommercialPaymentRequest;
import com.aeg.core.commercial.dto.CommercialPaymentResponse;

public interface CommercialPaymentService {

    List<CommercialPaymentResponse> findAll(Long facturaComercialId);

    CommercialPaymentResponse findById(Long id);

    CommercialPaymentResponse create(CommercialPaymentRequest request);

    CommercialPaymentResponse update(Long id, CommercialPaymentRequest request);

    void delete(Long id);
}
