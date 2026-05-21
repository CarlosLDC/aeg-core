package com.aeg.core.distributor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributorRepository extends JpaRepository<Distributor, Long> {

	List<Distributor> findByBranch_IdIn(Collection<Long> branchIds);

	Optional<Distributor> findByBranch_Id(Long branchId);
}