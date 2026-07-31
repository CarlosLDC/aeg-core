package com.aeg.core.firmware;

import java.time.OffsetDateTime;

import com.aeg.core.printermodel.PrinterModel;

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
@Table(name = "firmwares")
public class Firmware {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "version", nullable = false)
	private String version;

	@Column(name = "nombre_archivo", nullable = false, unique = true)
	private String fileName;

	@Column(name = "tamano_bytes", nullable = false)
	private Long sizeBytes;

	@Column(name = "checksum_sha256", nullable = false)
	private String checksumSha256;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_modelo_impresora")
	private PrinterModel printerModel;

	@Column(name = "notas")
	private String notes;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Long getSizeBytes() {
		return sizeBytes;
	}

	public void setSizeBytes(Long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}

	public String getChecksumSha256() {
		return checksumSha256;
	}

	public void setChecksumSha256(String checksumSha256) {
		this.checksumSha256 = checksumSha256;
	}

	public PrinterModel getPrinterModel() {
		return printerModel;
	}

	public void setPrinterModel(PrinterModel printerModel) {
		this.printerModel = printerModel;
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

	@PrePersist
	public void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
	}
}
