package com.aeg.core.modificationrequest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModificationRequestRepository extends JpaRepository<ModificationRequest, Long> {

	List<ModificationRequest> findByStatusOrderByCreatedAtDesc(ModificationRequestStatus status);

	List<ModificationRequest> findByTargetTypeAndStatusOrderByCreatedAtDesc(
			ModificationTargetType targetType,
			ModificationRequestStatus status);

	Optional<ModificationRequest> findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
			ModificationTargetType targetType,
			Long targetId,
			ModificationRequestStatus status);

	@Query("""
			select mr.id
			from ModificationRequest mr
			where mr.targetType = :targetType
			and mr.targetId = :targetId
			and mr.status = com.aeg.core.modificationrequest.ModificationRequestStatus.PENDING
			order by mr.createdAt desc
			""")
	List<Long> findPendingRequestIdsByTarget(ModificationTargetType targetType, Long targetId);

	void deleteByRequestedBy_Id(Long requestedById);
}
