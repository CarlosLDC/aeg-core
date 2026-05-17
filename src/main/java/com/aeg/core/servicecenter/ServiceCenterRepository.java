package com.aeg.core.servicecenter;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCenterRepository extends JpaRepository<ServiceCenter, Long> {

	List<ServiceCenter> findByBranch_IdIn(Collection<Long> branchIds);
}