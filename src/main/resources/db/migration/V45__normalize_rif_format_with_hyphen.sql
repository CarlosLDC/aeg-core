-- Migration V45: Normalize RIF format in empresas and related data to always have a hyphen after the initial letter (e.g. J-123456789)

DO $$
DECLARE
    r RECORD;
    v_canonical_id BIGINT;
    v_dup_id BIGINT;
BEGIN
    -- 1. Deduplicate empresas that differ only by the presence or absence of a hyphen
    FOR r IN (
        SELECT 
            UPPER(REPLACE(rif, '-', '')) as norm_rif,
            array_agg(id ORDER BY (rif LIKE '%-%') DESC, id ASC) as id_list
        FROM empresas
        GROUP BY UPPER(REPLACE(rif, '-', ''))
        HAVING count(*) > 1
    ) LOOP
        -- The first in the list is the canonical ID (prefers existing hyphen, then lowest ID)
        v_canonical_id := r.id_list[1];
        
        -- Loop over all duplicate IDs in the group and re-link foreign keys
        FOR i IN 2..array_length(r.id_list, 1) LOOP
            v_dup_id := r.id_list[i];
            
            -- Re-point any sucursales to the canonical empresa
            UPDATE sucursales
            SET id_empresa = v_canonical_id
            WHERE id_empresa = v_dup_id;
            
            -- Delete the duplicate empresa
            DELETE FROM empresas
            WHERE id = v_dup_id;
        END LOOP;
    END LOOP;

    -- 2. Format all remaining empresas.rif to standard format: [VEJPG]-[0-9]+
    UPDATE empresas
    SET rif = UPPER(SUBSTRING(REGEXP_REPLACE(rif, '[^A-Za-z0-9]', '', 'g'), 1, 1)) || '-' || SUBSTRING(REGEXP_REPLACE(rif, '[^A-Za-z0-9]', '', 'g'), 2)
    WHERE rif !~ '^[A-Za-z]-[0-9]+$';

    -- 3. Format proposed_data->'rif' in modification_requests if present
    UPDATE modification_requests
    SET proposed_data = jsonb_set(
        proposed_data,
        '{rif}',
        to_jsonb(
            UPPER(SUBSTRING(REGEXP_REPLACE(proposed_data->>'rif', '[^A-Za-z0-9]', '', 'g'), 1, 1)) || '-' || 
            SUBSTRING(REGEXP_REPLACE(proposed_data->>'rif', '[^A-Za-z0-9]', '', 'g'), 2)
        )
    )
    WHERE proposed_data ? 'rif' 
      AND proposed_data->>'rif' IS NOT NULL 
      AND proposed_data->>'rif' !~ '^[A-Za-z]-[0-9]+$';

    -- 4. Add CHECK constraint on empresas.rif if not already present
    IF NOT EXISTS (
        SELECT 1 
        FROM pg_constraint 
        WHERE conname = 'chk_empresas_rif_format'
    ) THEN
        ALTER TABLE empresas 
            ADD CONSTRAINT chk_empresas_rif_format 
            CHECK (rif ~ '^[VEJPG]-[0-9]{7,9}$');
    END IF;
END $$;
