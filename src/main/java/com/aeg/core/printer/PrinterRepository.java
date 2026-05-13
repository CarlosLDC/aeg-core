package com.aeg.core.printer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrinterRepository extends JpaRepository<Printer, Long> {
    boolean existsByFiscalSerialIgnoreCase(String fiscalSerial);
}
