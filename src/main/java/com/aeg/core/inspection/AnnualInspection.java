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
	@JoinColumn(name = "id_usuario", nullable = false)
	private com.aeg.core.security.User inspectorUser;

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

	@Column(name = "mqtt_registro_impresora")
	private String mqttRegistroImpresora;

	@Column(name = "mqtt_set_date_rev_o_at")
	private Long mqttSetDateRevOAt;

	@Column(name = "mqtt_numero_factura_prueba")
	private Integer mqttNumeroFacturaPrueba;

	@Column(name = "chk_precinto")
	private Boolean chkPrecinto;

	@Column(name = "chk_etiqueta_fiscal")
	private Boolean chkEtiquetaFiscal;

	@Column(name = "chk_factura")
	private Boolean chkFactura;

	@Column(name = "chk_nota_credito")
	private Boolean chkNotaCredito;

	@Column(name = "chk_sensor_papel")
	private Boolean chkSensorPapel;

	@Column(name = "mqtt_qr_codigo")
	private String mqttQrCodigo;

	@Column(name = "mqtt_qr_registro")
	private String mqttQrRegistro;

	@Column(name = "mqtt_qr_mac")
	private String mqttQrMac;

	@Column(name = "mqtt_qr_fecha")
	private String mqttQrFecha;

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

	public com.aeg.core.security.User getInspectorUser() {
		return inspectorUser;
	}

	public void setInspectorUser(com.aeg.core.security.User inspectorUser) {
		this.inspectorUser = inspectorUser;
	}

	public Long getUserId() {
		return inspectorUser == null ? null : inspectorUser.getId();
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

	public String getMqttRegistroImpresora() {
		return mqttRegistroImpresora;
	}

	public void setMqttRegistroImpresora(String mqttRegistroImpresora) {
		this.mqttRegistroImpresora = mqttRegistroImpresora;
	}

	public Long getMqttSetDateRevOAt() {
		return mqttSetDateRevOAt;
	}

	public void setMqttSetDateRevOAt(Long mqttSetDateRevOAt) {
		this.mqttSetDateRevOAt = mqttSetDateRevOAt;
	}

	public Integer getMqttNumeroFacturaPrueba() {
		return mqttNumeroFacturaPrueba;
	}

	public void setMqttNumeroFacturaPrueba(Integer mqttNumeroFacturaPrueba) {
		this.mqttNumeroFacturaPrueba = mqttNumeroFacturaPrueba;
	}

	public Boolean getChkPrecinto() {
		return chkPrecinto;
	}

	public void setChkPrecinto(Boolean chkPrecinto) {
		this.chkPrecinto = chkPrecinto;
	}

	public Boolean getChkEtiquetaFiscal() {
		return chkEtiquetaFiscal;
	}

	public void setChkEtiquetaFiscal(Boolean chkEtiquetaFiscal) {
		this.chkEtiquetaFiscal = chkEtiquetaFiscal;
	}

	public Boolean getChkFactura() {
		return chkFactura;
	}

	public void setChkFactura(Boolean chkFactura) {
		this.chkFactura = chkFactura;
	}

	public Boolean getChkNotaCredito() {
		return chkNotaCredito;
	}

	public void setChkNotaCredito(Boolean chkNotaCredito) {
		this.chkNotaCredito = chkNotaCredito;
	}

	public Boolean getChkSensorPapel() {
		return chkSensorPapel;
	}

	public void setChkSensorPapel(Boolean chkSensorPapel) {
		this.chkSensorPapel = chkSensorPapel;
	}

	public String getMqttQrCodigo() {
		return mqttQrCodigo;
	}

	public void setMqttQrCodigo(String mqttQrCodigo) {
		this.mqttQrCodigo = mqttQrCodigo;
	}

	public String getMqttQrRegistro() {
		return mqttQrRegistro;
	}

	public void setMqttQrRegistro(String mqttQrRegistro) {
		this.mqttQrRegistro = mqttQrRegistro;
	}

	public String getMqttQrMac() {
		return mqttQrMac;
	}

	public void setMqttQrMac(String mqttQrMac) {
		this.mqttQrMac = mqttQrMac;
	}

	public String getMqttQrFecha() {
		return mqttQrFecha;
	}

	public void setMqttQrFecha(String mqttQrFecha) {
		this.mqttQrFecha = mqttQrFecha;
	}
}
