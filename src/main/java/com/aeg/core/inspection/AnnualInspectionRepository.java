package com.aeg.core.inspection;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnualInspectionRepository extends JpaRepository<AnnualInspection, Long> {

	List<AnnualInspection> findByPrinter_IdIn(Collection<Long> printerIds);

	List<AnnualInspection> findByPrinter_IdOrderByCreatedAtAsc(Long printerId);

	boolean existsByInspectorUser_Id(Long userId);
}
