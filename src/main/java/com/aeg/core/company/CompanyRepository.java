package com.aeg.core.company;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByRif(String rif);

    Optional<Company> findByRif(String rif);

    @Query("""
            SELECT c FROM Company c
            WHERE UPPER(REPLACE(REPLACE(REPLACE(c.rif, '-', ''), ' ', ''), '.', '')) = :normalized
            """)
    Optional<Company> findByNormalizedRif(@Param("normalized") String normalized);

    @Query("SELECT DISTINCT c.branch.company FROM Client c WHERE c.distributor.id = :distributorId")
    List<Company> findCompaniesByDistributorId(@Param("distributorId") Long distributorId);
}
