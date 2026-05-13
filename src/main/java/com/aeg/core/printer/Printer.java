package com.aeg.core.printer;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "impresoras")
public class Printer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modelo_impresora")
    private com.aeg.core.printermodel.PrinterModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_software")
    private com.aeg.core.software.Software software;

    // id_compra (purchase) intentionally omitted per request

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private com.aeg.core.branch.Branch branch;

    @Column(name = "serial_fiscal", nullable = false, unique = true)
    private String fiscalSerial;

    @Column(name = "precio_venta_final")
    private BigDecimal finalSalePrice;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Convert(converter = PrinterStatusConverter.class)
    @Column(name = "estatus", nullable = false)
    private PrinterStatus status;

    // id_firmware intentionally ignored per request

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_distribuidora")
    private com.aeg.core.distributor.Distributor distributor;

    @Column(name = "se_pago", nullable = false)
    private Boolean paid = false;

    @Column(name = "fecha_instalacion")
    private OffsetDateTime installationDate;

    @Column(name = "version_firmware")
    private String versionFirmware;

    @Column(name = "direccion_mac")
    private String macAddress;

    @Convert(converter = DeviceTypeConverter.class)
    @Column(name = "tipo_dispositivo", nullable = false)
    private DeviceType deviceType;

    // Getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public com.aeg.core.printermodel.PrinterModel getModel() { return model; }
    public void setModel(com.aeg.core.printermodel.PrinterModel model) { this.model = model; }
    public Long getModelId() { return model == null ? null : model.getId(); }

    public com.aeg.core.software.Software getSoftware() { return software; }
    public void setSoftware(com.aeg.core.software.Software software) { this.software = software; }
    public Long getSoftwareId() { return software == null ? null : software.getId(); }

    // purchaseId intentionally omitted

    public com.aeg.core.branch.Branch getBranch() { return branch; }
    public void setBranch(com.aeg.core.branch.Branch branch) { this.branch = branch; }
    public Long getBranchId() { return branch == null ? null : branch.getId(); }

    public String getFiscalSerial() { return fiscalSerial; }
    public void setFiscalSerial(String fiscalSerial) { this.fiscalSerial = fiscalSerial; }

    public BigDecimal getFinalSalePrice() { return finalSalePrice; }
    public void setFinalSalePrice(BigDecimal finalSalePrice) { this.finalSalePrice = finalSalePrice; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public PrinterStatus getStatus() { return status; }
    public void setStatus(PrinterStatus status) { this.status = status; }

    public com.aeg.core.distributor.Distributor getDistributor() { return distributor; }
    public void setDistributor(com.aeg.core.distributor.Distributor distributor) { this.distributor = distributor; }
    public Long getDistributorId() { return distributor == null ? null : distributor.getId(); }

    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }

    public OffsetDateTime getInstallationDate() { return installationDate; }
    public void setInstallationDate(OffsetDateTime installationDate) { this.installationDate = installationDate; }

    public String getVersionFirmware() { return versionFirmware; }
    public void setVersionFirmware(String versionFirmware) { this.versionFirmware = versionFirmware; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (paid == null) paid = false;
        if (status == null) status = PrinterStatus.LABORATORIO;
        if (deviceType == null) deviceType = DeviceType.INTERNO;
    }
}
