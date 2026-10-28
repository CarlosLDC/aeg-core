-- V54: Desarrollador del software (nombre y RIF) directo en la tabla software sin FK a empresas
ALTER TABLE public.software
    ADD COLUMN IF NOT EXISTS nombre_desarrollador TEXT NULL,
    ADD COLUMN IF NOT EXISTS rif_desarrollador TEXT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'software'
          AND column_name = 'id_empresa_desarrolladora'
    ) THEN
        UPDATE public.software s
        SET nombre_desarrollador = COALESCE(s.nombre_desarrollador, e.razon_social),
            rif_desarrollador = COALESCE(s.rif_desarrollador, e.rif)
        FROM public.empresas e
        WHERE s.id_empresa_desarrolladora = e.id;

        DROP INDEX IF EXISTS public.idx_software_id_empresa_desarrolladora;

        ALTER TABLE public.software
            DROP COLUMN id_empresa_desarrolladora;
    END IF;
END $$;
