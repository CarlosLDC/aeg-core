package com.aeg.core.commercial;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotorComercialRepository extends JpaRepository<PromotorComercial, Long> {

    List<PromotorComercial> findAllByOrderByNombreAsc();

    List<PromotorComercial> findAllByActivoTrueOrderByNombreAsc();

    Optional<PromotorComercial> findByNombreIgnoreCase(String nombre);
}
