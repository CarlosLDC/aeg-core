package com.aeg.core.inspection;

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
@Table(name = "inspecciones_anuales", schema = "public")
public class AnnualInspection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_impresora", nullable = false)
	private com.aeg.core.printer.Printer printer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_empleado", nullable = false)
	private com.aeg.core.employee.Employee employee;

	@Column(name = "precinto_violentado", nullable = false)
	private Boolean sealTampered;

	@Column(name = "observaciones")
	private String notes;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "url_fotos", nullable = false)
	private String[] photoUrls;

	@Column(name = "fecha", nullable = false)
	private LocalDate inspectionDate;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
		if (photoUrls == null) {
			photoUrls = new String[0];
		}
		if (inspectionDate == null) {
			inspectionDate = LocalDate.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public com.aeg.core.printer.Printer getPrinter() {
		return printer;
	}

	public void setPrinter(com.aeg.core.printer.Printer printer) {
		this.printer = printer;
	}

	public Long getPrinterId() {
		return printer == null ? null : printer.getId();
	}

	public com.aeg.core.employee.Employee getEmployee() {
		return employee;
	}

	public void setEmployee(com.aeg.core.employee.Employee employee) {
		this.employee = employee;
	}

	public Long getEmployeeId() {
		return employee == null ? null : employee.getId();
	}

	public Boolean getSealTampered() {
		return sealTampered;
	}

	public void setSealTampered(Boolean sealTampered) {
		this.sealTampered = sealTampered;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String[] getPhotoUrls() {
		return photoUrls;
	}

	public void setPhotoUrls(String[] photoUrls) {
		this.photoUrls = photoUrls;
	}

	public LocalDate getInspectionDate() {
		return inspectionDate;
	}

	public void setInspectionDate(LocalDate inspectionDate) {
		this.inspectionDate = inspectionDate;
	}
}
