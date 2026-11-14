package com.aeg.core.commercial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PagoComercialRepository extends JpaRepository<PagoComercial, Long> {

    List<PagoComercial> findAllByOrderByCreatedAtDesc();

    List<PagoComercial> findByFacturaComercial_Id(Long facturaComercialId);

    List<PagoComercial> findByFacturaComercial_IdOrderByCreatedAtDesc(Long facturaComercialId);

    long countByFacturaComercial_Id(Long facturaComercialId);

    @Query("""
            SELECT p.facturaComercial.id, COUNT(p), COALESCE(SUM(p.monto), 0)
            FROM PagoComercial p
            GROUP BY p.facturaComercial.id
            """)
    List<Object[]> summarizePaymentsByFacturaComercialId();

    List<PagoComercial> findByMetodoBanco(String metodoBanco);
}
