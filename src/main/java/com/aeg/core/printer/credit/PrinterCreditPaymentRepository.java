package com.aeg.core.printer.credit;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrinterCreditPaymentRepository extends JpaRepository<PrinterCreditPayment, Long> {

    List<PrinterCreditPayment> findByPrinter_IdOrderByPaymentDateDescIdDesc(Long printerId);

    @Query("""
            SELECT p.printer.id, COUNT(p), COALESCE(SUM(p.amount), 0)
            FROM PrinterCreditPayment p
            WHERE p.printer.id IN :printerIds
            GROUP BY p.printer.id
            """)
    List<Object[]> summarizeByPrinterIds(@Param("printerIds") Collection<Long> printerIds);
}
