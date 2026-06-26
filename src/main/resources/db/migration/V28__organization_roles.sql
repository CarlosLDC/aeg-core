ALTER TABLE empresas
    ADD COLUMN organization_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

CREATE UNIQUE INDEX idx_empresas_single_factory
    ON empresas (organization_type)
    WHERE organization_type = 'FACTORY';

ALTER TABLE sucursales
    ADD COLUMN organization_role VARCHAR(20) NOT NULL DEFAULT 'NONE';

UPDATE sucursales
SET organization_role = CASE
    WHEN es_distribuidora = true AND es_centro_servicio = true THEN 'SERVICE_CENTER'
    WHEN es_distribuidora = true THEN 'DISTRIBUTOR'
    WHEN es_centro_servicio = true THEN 'SERVICE_CENTER'
    ELSE 'NONE'
END;

UPDATE empresas
SET organization_type = 'FACTORY'
WHERE UPPER(razon_social) LIKE '%AEG%'
   OR UPPER(rif) LIKE '%AEG%';
