package com.aeg.core.printer;

import java.util.List;

import com.aeg.core.printer.dto.PrinterDispositionRequest;
import com.aeg.core.printer.dto.PrinterRequest;
import com.aeg.core.printer.dto.PrinterResponse;

public interface PrinterService {
    List<PrinterResponse> findAll();
    PrinterResponse findById(Long id);
    PrinterResponse create(PrinterRequest request);
    PrinterResponse update(Long id, PrinterRequest request);
    PrinterResponse dispose(Long id, PrinterDispositionRequest request);
    void delete(Long id);
}
