package com.aeg.core.technicalservice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechnicalServiceVisitRepository extends JpaRepository<TechnicalServiceVisit, Long> {

	@EntityGraph(attributePaths = {
			"reviewedByUser",
			"serviceCenter",
			"serviceCenter.branch",
			"serviceCenter.branch.company",
			"distributor",
			"distributor.branch",
			"distributor.branch.company",
			"installedSeal",
			"removedSeal"
	})
	@Query("SELECT v FROM TechnicalServiceVisit v WHERE v.id = :id")
	Optional<TechnicalServiceVisit> findWithRelationsById(@Param("id") Long id);

	@EntityGraph(attributePaths = {
			"reviewedByUser",
			"serviceCenter",
			"serviceCenter.branch",
			"serviceCenter.branch.company",
			"distributor",
			"distributor.branch",
			"distributor.branch.company",
			"installedSeal",
			"removedSeal"
	})
	@Query("SELECT v FROM TechnicalServiceVisit v")
	List<TechnicalServiceVisit> findAllWithRelations();

	@EntityGraph(attributePaths = {
			"reviewedByUser",
			"serviceCenter",
			"serviceCenter.branch",
			"serviceCenter.branch.company",
			"distributor",
			"distributor.branch",
			"distributor.branch.company",
			"installedSeal",
			"removedSeal"
	})
	@Query("SELECT v FROM TechnicalServiceVisit v WHERE v.printer.id IN :printerIds")
	List<TechnicalServiceVisit> findByPrinter_IdIn(@Param("printerIds") Collection<Long> printerIds);

	@EntityGraph(attributePaths = {
			"reviewedByUser",
			"serviceCenter",
			"serviceCenter.branch",
			"serviceCenter.branch.company",
			"distributor",
			"distributor.branch",
			"distributor.branch.company",
			"installedSeal",
			"removedSeal"
	})
	List<TechnicalServiceVisit> findByPrinter_IdOrderByCreatedAtAsc(Long printerId);
}
