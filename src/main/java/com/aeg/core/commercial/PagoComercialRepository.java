package com.aeg.core.commercial;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PagoComercialRepository extends JpaRepository<PagoComercial, Long> {

    @EntityGraph(attributePaths = {"facturaComercial", "facturaComercial.clienteComercial"})
    List<PagoComercial> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"facturaComercial", "facturaComercial.clienteComercial"})
    Optional<PagoComercial> findById(Long id);

    List<PagoComercial> findByFacturaComercial_Id(Long facturaComercialId);

    @EntityGraph(attributePaths = {"facturaComercial", "facturaComercial.clienteComercial"})
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
