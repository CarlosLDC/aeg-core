package com.aeg.core.printermodel;

import java.util.List;

import com.aeg.core.printermodel.dto.PrinterModelRequest;
import com.aeg.core.printermodel.dto.PrinterModelResponse;

public interface PrinterModelService {
    List<PrinterModelResponse> findAll();
    PrinterModelResponse findById(Long id);
    PrinterModelResponse create(PrinterModelRequest request);
    PrinterModelResponse update(Long id, PrinterModelRequest request);
    void delete(Long id);
}
