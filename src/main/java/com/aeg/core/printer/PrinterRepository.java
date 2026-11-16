package com.aeg.core.printer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrinterRepository extends JpaRepository<Printer, Long> {
    boolean existsByFiscalSerialIgnoreCase(String fiscalSerial);

    @Query("""
            SELECT p FROM Printer p
            LEFT JOIN FETCH p.client c
            LEFT JOIN FETCH c.branch b
            LEFT JOIN FETCH b.company
            WHERE UPPER(p.fiscalSerial) = UPPER(:fiscalSerial)
            """)
    Optional<Printer> findEnajenacionCandidateByFiscalSerial(@Param("fiscalSerial") String fiscalSerial);
    boolean existsByClient_Id(Long clientId);

    @Query("""
            SELECT p FROM Printer p
            WHERE REPLACE(UPPER(TRIM(p.macAddress)), ':', '') = :compactMac
            """)
    Optional<Printer> findByMacAddressCompact(@Param("compactMac") String compactMac);

    @Query("""
            SELECT p.id FROM Printer p
            WHERE REPLACE(UPPER(TRIM(p.macAddress)), ':', '') = :compactMac
            """)
    Optional<Long> findIdByMacAddressCompact(@Param("compactMac") String compactMac);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM Printer p
            WHERE REPLACE(UPPER(TRIM(p.macAddress)), ':', '') = :compactMac
              AND (:excludeId IS NULL OR p.id <> :excludeId)
            """)
    boolean existsByMacAddressCompactExcludingId(
            @Param("compactMac") String compactMac,
            @Param("excludeId") Long excludeId);

    /** Navega la relación {@code distributor}, no un atributo {@code distributorId}. */
    List<Printer> findByDistributor_Id(Long distributorId);

    List<Printer> findByStatus(PrinterStatus status);

    @Query("""
            SELECT p FROM Printer p
            WHERE (p.client IS NOT NULL AND p.client.branch.id IN :branchIds)
               OR (p.distributor IS NOT NULL AND p.distributor.branch.id IN :branchIds)
            """)
    List<Printer> findByVisibleBranchIds(@Param("branchIds") Collection<Long> branchIds);

    long countByBox_Id(Long boxId);

    @Query("""
            SELECT p.box.id, COUNT(p)
            FROM Printer p
            WHERE p.box IS NOT NULL
            GROUP BY p.box.id
            """)
    List<Object[]> countPrintersGroupedByBoxId();

    @Modifying
    @Query("UPDATE Printer p SET p.box = NULL WHERE p.box.id = :boxId")
    int clearBoxByBoxId(@Param("boxId") Long boxId);

    long countByFacturaComercial_Id(Long facturaComercialId);

    List<Printer> findByFacturaComercial_IdOrderByFiscalSerialAsc(Long facturaComercialId);

    @Query("""
            SELECT p.id, f.id, f.numeroFactura
            FROM Printer p
            JOIN p.facturaComercial f
            """)
    List<Object[]> findAllPrinterCommercialInvoiceRefs();

    @Query("""
            SELECT f.id, f.numeroFactura
            FROM Printer p
            JOIN p.facturaComercial f
            WHERE p.id = :printerId
            """)
    List<Object[]> findPrinterCommercialInvoiceRef(@Param("printerId") Long printerId);

    @Query("""
            SELECT p.facturaComercial.id, COUNT(p)
            FROM Printer p
            WHERE p.facturaComercial IS NOT NULL
            GROUP BY p.facturaComercial.id
            """)
    List<Object[]> countPrintersGroupedByFacturaComercialId();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Printer p SET p.facturaComercial = NULL WHERE p.facturaComercial.id = :facturaId")
    int clearFacturaComercialByFacturaId(@Param("facturaId") Long facturaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Printer p SET p.facturaComercial = :factura WHERE p.id IN :printerIds")
    int assignFacturaComercialByPrinterIds(
            @Param("factura") com.aeg.core.commercial.FacturaComercial factura,
            @Param("printerIds") Collection<Long> printerIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Printer p SET p.facturaComercial = NULL WHERE p.facturaComercial.id = :facturaId AND p.id = :printerId")
    int clearFacturaComercialByFacturaIdAndPrinterId(
            @Param("facturaId") Long facturaId,
            @Param("printerId") Long printerId);
}
