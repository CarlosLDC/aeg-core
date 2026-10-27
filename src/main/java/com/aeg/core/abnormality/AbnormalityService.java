package com.aeg.core.abnormality;

import java.util.List;

import com.aeg.core.abnormality.dto.AbnormalityRequest;
import com.aeg.core.abnormality.dto.AbnormalityResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.security.User;

public interface AbnormalityService {

    List<AbnormalityResponse> findAll();

    AbnormalityResponse create(AbnormalityRequest request);

    /**
     * Registra una anormalidad generada automáticamente (idempotente por origen, id de origen y tipo).
     */
    void record(Printer printer, AbnormalityType type, AbnormalityOrigin origin, Long originId,
            User user, String description);
}
