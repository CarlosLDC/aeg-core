package com.aeg.core.technicalservice;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechnicalServiceVisitRepository extends JpaRepository<TechnicalServiceVisit, Long> {

	@Query("SELECT v FROM TechnicalServiceVisit v WHERE v.printer.id IN :printerIds")
	List<TechnicalServiceVisit> findByPrinter_IdIn(@Param("printerIds") Collection<Long> printerIds);

	List<TechnicalServiceVisit> findByPrinter_IdOrderByCreatedAtAsc(Long printerId);
}
