package com.aeg.core.printer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
            WHERE REPLACE(UPPER(p.macAddress), ':', '') = :compactMac
            """)
    Optional<Printer> findByMacAddressCompact(@Param("compactMac") String compactMac);

    /** Navega la relación {@code distributor}, no un atributo {@code distributorId}. */
    List<Printer> findByDistributor_Id(Long distributorId);

    List<Printer> findByStatus(PrinterStatus status);

    @Query("""
            SELECT p FROM Printer p
            WHERE (p.client IS NOT NULL AND p.client.branch.id IN :branchIds)
               OR (p.distributor IS NOT NULL AND p.distributor.branch.id IN :branchIds)
            """)
    List<Printer> findByVisibleBranchIds(@Param("branchIds") Collection<Long> branchIds);
}
