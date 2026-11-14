package com.aeg.core.commercial;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaComercialRepository extends JpaRepository<FacturaComercial, Long> {

    @EntityGraph(attributePaths = {"clienteComercial", "promotor"})
    List<FacturaComercial> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"clienteComercial", "promotor"})
    Optional<FacturaComercial> findById(Long id);

    List<FacturaComercial> findByNumeroFactura(String numeroFactura);

    List<FacturaComercial> findByModalidad(String modalidad);

    List<FacturaComercial> findByEstadoCobro(String estadoCobro);
}
