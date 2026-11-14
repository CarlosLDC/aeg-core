package com.aeg.core.commercial;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteComercialRepository extends JpaRepository<ClienteComercial, Long> {

    List<ClienteComercial> findAllByOrderByNombreAsc();

    List<ClienteComercial> findAllByActivoTrueOrderByNombreAsc();

    Optional<ClienteComercial> findByNombreIgnoreCase(String nombre);
}
