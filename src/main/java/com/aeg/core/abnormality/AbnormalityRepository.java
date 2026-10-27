package com.aeg.core.abnormality;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AbnormalityRepository extends JpaRepository<Abnormality, Long> {

    List<Abnormality> findByPrinter_IdInOrderByDetectedAtDesc(Collection<Long> printerIds);

    List<Abnormality> findAllByOrderByDetectedAtDesc();

    boolean existsByOriginAndOriginIdAndType(AbnormalityOrigin origin, Long originId, AbnormalityType type);
}
