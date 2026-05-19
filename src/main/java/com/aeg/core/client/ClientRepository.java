package com.aeg.core.client;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
	/** Navega la relación {@code distributor}, no un atributo {@code distributorId}. */
	List<Client> findByDistributor_Id(Long distributorId);

	List<Client> findByBranch_IdIn(java.util.Collection<Long> branchIds);

	Optional<Client> findByBranch_Id(Long branchId);
}