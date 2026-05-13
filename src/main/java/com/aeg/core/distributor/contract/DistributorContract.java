package com.aeg.core.distributor.contract;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "contratos_distribuidoras", schema = "public")
public class DistributorContract {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate startDate;

	@Column(name = "fecha_fin", nullable = false)
	private LocalDate endDate;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_distribuidora", nullable = false)
	private com.aeg.core.distributor.Distributor distributor;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "url_fotos", nullable = false)
	private String[] photoUrls;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
		if (photoUrls == null) {
			photoUrls = new String[0];
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public com.aeg.core.distributor.Distributor getDistributor() {
		return distributor;
	}

	public void setDistributor(com.aeg.core.distributor.Distributor distributor) {
		this.distributor = distributor;
	}

	public Long getDistributorId() {
		return distributor == null ? null : distributor.getId();
	}

	public String[] getPhotoUrls() {
		return photoUrls;
	}

	public void setPhotoUrls(String[] photoUrls) {
		this.photoUrls = photoUrls;
	}
}
