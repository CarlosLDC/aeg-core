package com.aeg.core.printer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrinterRepository extends JpaRepository<Printer, Long> {
    boolean existsByFiscalSerialIgnoreCase(String fiscalSerial);

    /** Navega la relación {@code distributor}, no un atributo {@code distributorId}. */
    List<Printer> findByDistributor_Id(Long distributorId);
}
