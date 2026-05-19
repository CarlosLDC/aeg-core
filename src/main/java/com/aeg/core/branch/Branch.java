package com.aeg.core.branch;

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

import java.time.OffsetDateTime;

@Entity
@Table(name = "sucursales")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private com.aeg.core.company.Company company;

    @Column(name = "ciudad", nullable = false)
    private String city;

    @Column(name = "estado", nullable = false)
    private String state;

    @Column(name = "direccion")
    private String address;

    @Column(name = "telefono")
    private String phone;

    @Column(name = "correo")
    private String email;

    @Column(name = "nombre_persona_contacto")
    private String contactPersonName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "es_cliente")
    private Boolean isClient = false;

    @Column(name = "es_distribuidora")
    private Boolean isDistributor = false;

    @Column(name = "es_centro_servicio")
    private Boolean isServiceCenter = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public com.aeg.core.company.Company getCompany() { return company; }
    public void setCompany(com.aeg.core.company.Company company) { this.company = company; }
    public Long getCompanyId() { return company == null ? null : company.getId(); }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getIsClient() { return isClient; }
    public void setIsClient(Boolean isClient) { this.isClient = isClient; }
    public Boolean getIsDistributor() { return isDistributor; }
    public void setIsDistributor(Boolean isDistributor) { this.isDistributor = isDistributor; }
    public Boolean getIsServiceCenter() { return isServiceCenter; }
    public void setIsServiceCenter(Boolean isServiceCenter) { this.isServiceCenter = isServiceCenter; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (isClient == null) isClient = false;
        if (isDistributor == null) isDistributor = false;
        if (isServiceCenter == null) isServiceCenter = false;
    }
}
