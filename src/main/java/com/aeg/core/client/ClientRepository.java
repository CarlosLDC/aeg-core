package com.aeg.core.client;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {
	/** Navega la relación {@code distributor}, no un atributo {@code distributorId}. */
	List<Client> findByDistributor_Id(Long distributorId);

	List<Client> findByBranch_IdIn(java.util.Collection<Long> branchIds);

	Optional<Client> findByBranch_Id(Long branchId);

	Optional<Client> findFirstByBranch_Id(Long branchId);

	List<Client> findAllByBranch_Id(Long branchId);

	@Query("""
			SELECT c FROM Client c
			LEFT JOIN FETCH c.branch
			LEFT JOIN FETCH c.distributor
			WHERE c.branch.id = :branchId
			ORDER BY c.id ASC
			""")
	List<Client> findAllFetchedByBranchId(@Param("branchId") Long branchId);
}