package com.aeg.core.modificationrequest;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.aeg.core.security.User;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "modification_requests", schema = "public")
public class ModificationRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "action_type", nullable = false)
	private ModificationActionType actionType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "proposed_data")
	private Map<String, Object> proposedData;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requested_by", nullable = false)
	private User requestedBy;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ModificationRequestStatus status = ModificationRequestStatus.PENDING;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
		if (status == null) {
			status = ModificationRequestStatus.PENDING;
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Long employeeId) {
		this.employeeId = employeeId;
	}

	public ModificationActionType getActionType() {
		return actionType;
	}

	public void setActionType(ModificationActionType actionType) {
		this.actionType = actionType;
	}

	public Map<String, Object> getProposedData() {
		return proposedData;
	}

	public void setProposedData(Map<String, Object> proposedData) {
		this.proposedData = proposedData;
	}

	public User getRequestedBy() {
		return requestedBy;
	}

	public void setRequestedBy(User requestedBy) {
		this.requestedBy = requestedBy;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public ModificationRequestStatus getStatus() {
		return status;
	}

	public void setStatus(ModificationRequestStatus status) {
		this.status = status;
	}
}
