package com.aeg.core.printermodel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrinterModelRepository extends JpaRepository<PrinterModel, Long> {

    Optional<PrinterModel> findFirstByModelCodeIgnoreCaseOrderByIdAsc(String modelCode);
}
