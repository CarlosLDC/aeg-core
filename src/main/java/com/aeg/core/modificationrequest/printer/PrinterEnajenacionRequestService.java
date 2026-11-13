package com.aeg.core.modificationrequest.printer;

import java.util.List;
import java.util.Optional;

import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionApproveRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionCreateRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRejectRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRequestDetailResponse;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRequestListItemResponse;

public interface PrinterEnajenacionRequestService {

    PrinterEnajenacionRequestDetailResponse createRequest(PrinterEnajenacionCreateRequest request);

    List<PrinterEnajenacionRequestListItemResponse> findByStatus(ModificationRequestStatus status);

    PrinterEnajenacionRequestDetailResponse findById(Long id);

    Optional<PrinterEnajenacionRequestDetailResponse> findPendingByPrinterId(Long printerId);

    PrinterEnajenacionRequestDetailResponse approve(Long id, PrinterEnajenacionApproveRequest request);

    PrinterEnajenacionRequestDetailResponse reject(Long id, PrinterEnajenacionRejectRequest request);

    PrinterEnajenacionRequestDetailResponse cancel(Long id);
}
