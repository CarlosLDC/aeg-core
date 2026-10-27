package com.aeg.core.software;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "id_empresa_desarrolladora")
    private com.aeg.core.company.Company developerCompany;

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

    public com.aeg.core.company.Company getDeveloperCompany() { return developerCompany; }
    public void setDeveloperCompany(com.aeg.core.company.Company developerCompany) { this.developerCompany = developerCompany; }

    public boolean isExternal() { return external; }
    public void setExternal(boolean external) { this.external = external; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
