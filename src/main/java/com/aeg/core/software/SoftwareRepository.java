package com.aeg.core.software;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SoftwareRepository extends JpaRepository<Software, Long> {

    boolean existsByNameIgnoreCaseAndVersionIgnoreCase(String name, String version);

    Optional<Software> findByNameIgnoreCaseAndVersionIgnoreCase(String name, String version);
}

