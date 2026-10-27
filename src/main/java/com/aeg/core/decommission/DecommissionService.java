package com.aeg.core.decommission;

import java.util.List;

import com.aeg.core.decommission.dto.DecommissionRequest;
import com.aeg.core.decommission.dto.DecommissionResponse;

public interface DecommissionService {

    /** Desincorpora la impresora: crea el registro y cambia su estatus a desincorporada. */
    DecommissionResponse decommission(Long printerId, DecommissionRequest request);

    List<DecommissionResponse> findByPrinter(Long printerId);
}
