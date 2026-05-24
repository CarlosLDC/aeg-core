package com.aeg.core.employee;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "empleados", schema = "public")
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "cedula", nullable = false, unique = true)
	private String nationalId;

	@Column(name = "nombre", nullable = false)
	private String name;

	@Column(name = "telefono", nullable = false)
	private String phone;

	@Column(name = "correo", nullable = false)
	private String email;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Convert(converter = EmployeeTypeConverter.class)
	@Column(name = "tipo", nullable = false)
	private EmployeeType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal", nullable = false)
	private com.aeg.core.branch.Branch branch;

	@Enumerated(EnumType.STRING)
	@Column(name = "review_status", nullable = false)
	private EmployeeReviewStatus reviewStatus = EmployeeReviewStatus.ACTIVE;

	@Version
	@Column(name = "version", nullable = false)
	private Long version = 0L;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNationalId() {
		return nationalId;
	}

	public void setNationalId(String nationalId) {
		this.nationalId = nationalId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public EmployeeType getType() {
		return type;
	}

	public void setType(EmployeeType type) {
		this.type = type;
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

	public EmployeeReviewStatus getReviewStatus() {
		return reviewStatus;
	}

	public void setReviewStatus(EmployeeReviewStatus reviewStatus) {
		this.reviewStatus = reviewStatus;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
