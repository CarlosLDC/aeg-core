package com.aeg.core.company;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "empresas", schema = "public")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razon_social")
    private String businessName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "rif", nullable = false, unique = true)
    private String rif;

    @jakarta.persistence.Convert(converter = ContributorTypeConverter.class)
    @Column(name = "tipo_contribuyente", nullable = false)
    private ContributorType contributorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false)
    private OrganizationType organizationType = OrganizationType.STANDARD;

    public static String toStandardRif(String rif) {
        if (rif == null || rif.isBlank()) {
            return rif;
        }
        String clean = rif.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (clean.length() > 1 && clean.matches("^[VEJPG][0-9]+$")) {
            return clean.charAt(0) + "-" + clean.substring(1);
        }
        return clean;
    }

    @PrePersist
    @jakarta.persistence.PreUpdate
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (organizationType == null) {
            organizationType = OrganizationType.STANDARD;
        }
        if (rif != null) {
            rif = toStandardRif(rif);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRif() {
        return rif;
    }

    public void setRif(String rif) {
        this.rif = toStandardRif(rif);
    }

    public ContributorType getContributorType() {
        return contributorType;
    }

    public void setContributorType(ContributorType contributorType) {
        this.contributorType = contributorType;
    }

    public OrganizationType getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(OrganizationType organizationType) {
        this.organizationType = organizationType;
    }
}
