package com.aeg.core.servicecenter;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceCenterRepository extends JpaRepository<ServiceCenter, Long> {

	List<ServiceCenter> findByBranch_IdIn(Collection<Long> branchIds);

	@Query("""
			SELECT s FROM ServiceCenter s
			JOIN FETCH s.branch
			WHERE s.branch.id IN :branchIds
			ORDER BY s.id ASC
			""")
	List<ServiceCenter> findAllFetchedByBranch_IdIn(@Param("branchIds") Collection<Long> branchIds);
}