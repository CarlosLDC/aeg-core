package com.aeg.core.technician;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

	@Query("SELECT t FROM Technician t WHERE t.employee.branch.id IN :branchIds")
	List<Technician> findByEmployee_Branch_IdIn(@Param("branchIds") Collection<Long> branchIds);

	Optional<Technician> findByEmployee_Id(Long employeeId);
}
