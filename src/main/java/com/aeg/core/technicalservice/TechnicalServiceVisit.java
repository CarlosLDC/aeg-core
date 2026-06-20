package com.aeg.core.technicalservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.aeg.core.distributor.Distributor;
import com.aeg.core.printer.Printer;
import com.aeg.core.seal.Seal;
import com.aeg.core.servicecenter.ServiceCenter;

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
public class TechnicalServiceVisit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_impresora", nullable = false)
	private Printer printer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", nullable = false)
	private com.aeg.core.security.User reviewedByUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_centro_servicio")
	private ServiceCenter serviceCenter;

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
	private Seal installedSeal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_precinto_retirado")
	private Seal removedSeal;

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
	private Distributor distributor;

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

	public Printer getPrinter() {
		return printer;
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}

	public Long getPrinterId() {
		return printer == null ? null : printer.getId();
	}

	public com.aeg.core.security.User getReviewedByUser() {
		return reviewedByUser;
	}

	public void setReviewedByUser(com.aeg.core.security.User reviewedByUser) {
		this.reviewedByUser = reviewedByUser;
	}

	public Long getUserId() {
		return reviewedByUser == null ? null : reviewedByUser.getId();
	}

	public ServiceCenter getServiceCenter() {
		return serviceCenter;
	}

	public void setServiceCenter(ServiceCenter serviceCenter) {
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

	public Seal getInstalledSeal() {
		return installedSeal;
	}

	public void setInstalledSeal(Seal installedSeal) {
		this.installedSeal = installedSeal;
	}

	public Long getInstalledSealId() {
		return installedSeal == null ? null : installedSeal.getId();
	}

	public Seal getRemovedSeal() {
		return removedSeal;
	}

	public void setRemovedSeal(Seal removedSeal) {
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

	public Distributor getDistributor() {
		return distributor;
	}

	public void setDistributor(Distributor distributor) {
		this.distributor = distributor;
	}

	public Long getDistributorId() {
		return distributor == null ? null : distributor.getId();
	}
}
