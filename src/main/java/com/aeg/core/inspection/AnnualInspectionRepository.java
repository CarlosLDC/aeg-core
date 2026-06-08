package com.aeg.core.inspection;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnualInspectionRepository extends JpaRepository<AnnualInspection, Long> {

	@Query("""
			SELECT i FROM AnnualInspection i
			WHERE i.printer.id IN :printerIds AND i.employee.branch.id IN :branchIds
			""")
	List<AnnualInspection> findByPrinter_IdInAndEmployee_Branch_IdIn(
			@Param("printerIds") Collection<Long> printerIds,
			@Param("branchIds") Collection<Long> branchIds);

	List<AnnualInspection> findByPrinter_IdIn(Collection<Long> printerIds);

	List<AnnualInspection> findByPrinter_IdOrderByCreatedAtAsc(Long printerId);

	boolean existsByEmployee_Id(Long employeeId);
}
