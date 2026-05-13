package com.aeg.core.seal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SealRepository extends JpaRepository<Seal, Long> {
    boolean existsBySerialIgnoreCase(String serial);
}
