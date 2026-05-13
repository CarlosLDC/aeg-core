package com.aeg.core.serviciotecnico;

import java.math.BigDecimal;
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
@Table(name = "servicios_tecnicos", schema = "public")
public class ServicioTecnico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_impresora", nullable = false)
	private com.aeg.core.printer.Printer printer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_tecnico", nullable = false)
	private com.aeg.core.tecnico.Tecnico technician;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_centro_servicio")
	private com.aeg.core.servicecenter.ServiceCenter serviceCenter;

	@Column(name = "precinto_violentado", nullable = false)
	private Boolean sealTampered;

	@Column(name = "observaciones")
	private String notes;

	@Column(name = "fecha_inicio", nullable = false)
	private OffsetDateTime startAt;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "fecha_fin", nullable = false)
	private OffsetDateTime endAt;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "url_fotos", nullable = false)
	private String[] photoUrls;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_precinto_instalado")
	private com.aeg.core.seal.Seal installedSeal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_precinto_retirado")
	private com.aeg.core.seal.Seal removedSeal;

	@Column(name = "reporte_z_inicial", nullable = false)
	private Integer initialZReport;

	@Column(name = "reporte_z_final", nullable = false)
	private Integer finalZReport;

	@Column(name = "costo", nullable = false, precision = 19, scale = 4)
	private BigDecimal cost;

	@Column(name = "falla_reportada", nullable = false)
	private String reportedFailure;

	@Column(name = "fecha_solicitud", nullable = false)
	private LocalDate requestDate;

	@Column(name = "fecha_z_inicial", nullable = false)
	private OffsetDateTime initialZDate;

	@Column(name = "fecha_z_final", nullable = false)
	private OffsetDateTime finalZDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_distribuidora")
	private com.aeg.core.distributor.Distributor distributor;

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

	public com.aeg.core.printer.Printer getPrinter() {
		return printer;
	}

	public void setPrinter(com.aeg.core.printer.Printer printer) {
		this.printer = printer;
	}

	public Long getPrinterId() {
		return printer == null ? null : printer.getId();
	}

	public com.aeg.core.tecnico.Tecnico getTechnician() {
		return technician;
	}

	public void setTechnician(com.aeg.core.tecnico.Tecnico technician) {
		this.technician = technician;
	}

	public Long getTechnicianId() {
		return technician == null ? null : technician.getId();
	}

	public com.aeg.core.servicecenter.ServiceCenter getServiceCenter() {
		return serviceCenter;
	}

	public void setServiceCenter(com.aeg.core.servicecenter.ServiceCenter serviceCenter) {
		this.serviceCenter = serviceCenter;
	}

	public Long getServiceCenterId() {
		return serviceCenter == null ? null : serviceCenter.getId();
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

	public OffsetDateTime getStartAt() {
		return startAt;
	}

	public void setStartAt(OffsetDateTime startAt) {
		this.startAt = startAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getEndAt() {
		return endAt;
	}

	public void setEndAt(OffsetDateTime endAt) {
		this.endAt = endAt;
	}

	public String[] getPhotoUrls() {
		return photoUrls;
	}

	public void setPhotoUrls(String[] photoUrls) {
		this.photoUrls = photoUrls;
	}

	public com.aeg.core.seal.Seal getInstalledSeal() {
		return installedSeal;
	}

	public void setInstalledSeal(com.aeg.core.seal.Seal installedSeal) {
		this.installedSeal = installedSeal;
	}

	public Long getInstalledSealId() {
		return installedSeal == null ? null : installedSeal.getId();
	}

	public com.aeg.core.seal.Seal getRemovedSeal() {
		return removedSeal;
	}

	public void setRemovedSeal(com.aeg.core.seal.Seal removedSeal) {
		this.removedSeal = removedSeal;
	}

	public Long getRemovedSealId() {
		return removedSeal == null ? null : removedSeal.getId();
	}

	public Integer getInitialZReport() {
		return initialZReport;
	}

	public void setInitialZReport(Integer initialZReport) {
		this.initialZReport = initialZReport;
	}

	public Integer getFinalZReport() {
		return finalZReport;
	}

	public void setFinalZReport(Integer finalZReport) {
		this.finalZReport = finalZReport;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

	public String getReportedFailure() {
		return reportedFailure;
	}

	public void setReportedFailure(String reportedFailure) {
		this.reportedFailure = reportedFailure;
	}

	public LocalDate getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(LocalDate requestDate) {
		this.requestDate = requestDate;
	}

	public OffsetDateTime getInitialZDate() {
		return initialZDate;
	}

	public void setInitialZDate(OffsetDateTime initialZDate) {
		this.initialZDate = initialZDate;
	}

	public OffsetDateTime getFinalZDate() {
		return finalZDate;
	}

	public void setFinalZDate(OffsetDateTime finalZDate) {
		this.finalZDate = finalZDate;
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
}
