package com.aeg.core.commercial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoComercialRepository extends JpaRepository<PagoComercial, Long> {

    List<PagoComercial> findByFacturaComercial_Id(Long facturaComercialId);

    List<PagoComercial> findByMetodoBanco(String metodoBanco);
}
