package com.aeg.core.commercial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaComercialRepository extends JpaRepository<FacturaComercial, Long> {

    List<FacturaComercial> findAllByOrderByCreatedAtDesc();

    List<FacturaComercial> findByNumeroFactura(String numeroFactura);

    List<FacturaComercial> findByModalidad(String modalidad);

    List<FacturaComercial> findByEstadoCobro(String estadoCobro);
}
