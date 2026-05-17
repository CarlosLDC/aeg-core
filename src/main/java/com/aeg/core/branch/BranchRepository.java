package com.aeg.core.branch;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<Branch, Long> {
	
	@Query("SELECT DISTINCT c.branch FROM Client c WHERE c.distributor.id = :distributorId")
	List<Branch> findBranchesByDistributorId(@Param("distributorId") Long distributorId);
}
