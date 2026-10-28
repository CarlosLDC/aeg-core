package com.aeg.core.software;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "software")
public class Software {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String name;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "nombre_desarrollador")
    private String developerName;

    @Column(name = "rif_desarrollador")
    private String developerRif;

    @Column(name = "es_externo", nullable = false)
    private boolean external = false;

    @ElementCollection
    @CollectionTable(name = "software_lenguajes_programacion", joinColumns = @JoinColumn(name = "software_id"))
    @Column(name = "lenguaje")
    private List<String> programmingLanguages = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "software_sistemas_operativos", joinColumns = @JoinColumn(name = "software_id"))
    @Column(name = "sistema")
    private List<String> operatingSystems = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "software_distribuidores",
            joinColumns = @JoinColumn(name = "software_id"),
            inverseJoinColumns = @JoinColumn(name = "id_empresa"))
    private List<com.aeg.core.company.Company> distributors = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public List<String> getProgrammingLanguages() { return programmingLanguages; }
    public void setProgrammingLanguages(List<String> programmingLanguages) { this.programmingLanguages = programmingLanguages; }
    public List<String> getOperatingSystems() { return operatingSystems; }
    public void setOperatingSystems(List<String> operatingSystems) { this.operatingSystems = operatingSystems; }

    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }

    public String getDeveloperRif() { return developerRif; }
    public void setDeveloperRif(String developerRif) { this.developerRif = developerRif; }

    public boolean isExternal() { return external; }
    public void setExternal(boolean external) { this.external = external; }

    public List<com.aeg.core.company.Company> getDistributors() { return distributors; }
    public void setDistributors(List<com.aeg.core.company.Company> distributors) { this.distributors = distributors; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
