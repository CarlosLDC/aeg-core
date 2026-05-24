package com.aeg.core.client;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes", schema = "public")
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal", nullable = false)
	private com.aeg.core.branch.Branch branch;

	@Column(name = "id_distribuidora")
	private Long distributorId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_distribuidora", insertable = false, updatable = false)
	private com.aeg.core.distributor.Distributor distributor;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "review_status", nullable = false)
	private ClientReviewStatus reviewStatus = ClientReviewStatus.ACTIVE;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (reviewStatus == null) {
			reviewStatus = ClientReviewStatus.ACTIVE;
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public com.aeg.core.branch.Branch getBranch() { return branch; }
	public void setBranch(com.aeg.core.branch.Branch branch) { this.branch = branch; }
	public Long getBranchId() { return branch == null ? null : branch.getId(); }
	public com.aeg.core.distributor.Distributor getDistributor() { return distributor; }
	public void setDistributor(com.aeg.core.distributor.Distributor distributor) {
		this.distributor = distributor;
		this.distributorId = distributor == null ? null : distributor.getId();
	}
	public Long getDistributorId() {
		return distributorId;
	}

	public void setDistributorId(Long distributorId) {
		this.distributorId = distributorId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public ClientReviewStatus getReviewStatus() {
		return reviewStatus;
	}

	public void setReviewStatus(ClientReviewStatus reviewStatus) {
		this.reviewStatus = reviewStatus;
	}
}