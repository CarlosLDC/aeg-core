package com.aeg.core.commercial;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaComercialRepository extends JpaRepository<FacturaComercial, Long> {

    @EntityGraph(attributePaths = {
            "company",
            "distributor", "distributor.branch", "distributor.branch.company",
            "client", "client.branch", "client.branch.company",
            "clienteComercial"
    })
    List<FacturaComercial> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {
            "company",
            "distributor", "distributor.branch", "distributor.branch.company",
            "client", "client.branch", "client.branch.company",
            "clienteComercial"
    })
    Optional<FacturaComercial> findById(Long id);

    List<FacturaComercial> findByNumeroFactura(String numeroFactura);

    List<FacturaComercial> findByModalidad(String modalidad);

    List<FacturaComercial> findByEstadoCobro(String estadoCobro);
}
