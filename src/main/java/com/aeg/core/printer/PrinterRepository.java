package com.aeg.core.printer;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrinterRepository extends JpaRepository<Printer, Long> {
    boolean existsByFiscalSerialIgnoreCase(String fiscalSerial);

    /** Navega la relación {@code distributor}, no un atributo {@code distributorId}. */
    List<Printer> findByDistributor_Id(Long distributorId);

    @Query("""
            SELECT p FROM Printer p
            WHERE (p.client IS NOT NULL AND p.client.branch.id IN :branchIds)
               OR (p.distributor IS NOT NULL AND p.distributor.branch.id IN :branchIds)
            """)
    List<Printer> findByVisibleBranchIds(@Param("branchIds") Collection<Long> branchIds);
}
