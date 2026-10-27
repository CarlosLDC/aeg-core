package com.aeg.core.servicecenter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "centros_servicio", schema = "public")
public class ServiceCenter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal", nullable = false)
	private com.aeg.core.branch.Branch branch;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "fecha_inclusion")
	private LocalDate inclusionDate;

	@Column(name = "fecha_exclusion")
	private LocalDate exclusionDate;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (inclusionDate == null) {
			inclusionDate = LocalDate.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public com.aeg.core.branch.Branch getBranch() {
		return branch;
	}

	public void setBranch(com.aeg.core.branch.Branch branch) {
		this.branch = branch;
	}

	public Long getBranchId() {
		return branch == null ? null : branch.getId();
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDate getInclusionDate() { return inclusionDate; }
	public void setInclusionDate(LocalDate inclusionDate) { this.inclusionDate = inclusionDate; }

	public LocalDate getExclusionDate() { return exclusionDate; }
	public void setExclusionDate(LocalDate exclusionDate) { this.exclusionDate = exclusionDate; }
}