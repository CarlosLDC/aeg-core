package com.aeg.core.distributorperson;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributorPersonRepository extends JpaRepository<DistributorPerson, Long> {

	Optional<DistributorPerson> findByEmployee_Id(Long employeeId);
}
