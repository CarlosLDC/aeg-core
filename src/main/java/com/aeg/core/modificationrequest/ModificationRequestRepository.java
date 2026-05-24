package com.aeg.core.modificationrequest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModificationRequestRepository extends JpaRepository<ModificationRequest, Long> {

	List<ModificationRequest> findByStatusOrderByCreatedAtDesc(ModificationRequestStatus status);

	Optional<ModificationRequest> findFirstByEmployeeIdAndStatusOrderByCreatedAtDesc(
			Long employeeId,
			ModificationRequestStatus status);

	@Query("""
			select mr.id
			from ModificationRequest mr
			where mr.employeeId = :employeeId
			and mr.status = com.aeg.core.modificationrequest.ModificationRequestStatus.PENDING
			order by mr.createdAt desc
			""")
	List<Long> findPendingRequestIdsByEmployeeId(Long employeeId);
}
