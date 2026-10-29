-- Reemplazar version_firmware (texto) por id_firmware (clave foránea a firmwares) en impresoras.

ALTER TABLE public.impresoras
    ADD COLUMN IF NOT EXISTS id_firmware BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'impresoras_id_firmware_fkey'
    ) THEN
        ALTER TABLE public.impresoras
            ADD CONSTRAINT impresoras_id_firmware_fkey
            FOREIGN KEY (id_firmware) REFERENCES public.firmwares (id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_impresoras_id_firmware ON public.impresoras (id_firmware);

-- Backfill: enlazar version_firmware con firmwares (priorizando el mismo modelo fiscal, luego genérico, luego cualquiera con esa versión)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'impresoras'
          AND column_name = 'version_firmware'
    ) THEN
        UPDATE public.impresoras i
        SET id_firmware = sub.id
        FROM (
            SELECT i2.id AS printer_id,
                   (
                       SELECT f.id
                       FROM public.firmwares f
                       WHERE f.version = TRIM(i2.version_firmware)
                         AND (f.id_modelo_impresora = i2.id_modelo_impresora OR f.id_modelo_impresora IS NULL)
                       ORDER BY (f.id_modelo_impresora IS NOT NULL) DESC, f.created_at DESC
                       LIMIT 1
                   ) AS id
            FROM public.impresoras i2
            WHERE i2.version_firmware IS NOT NULL AND TRIM(i2.version_firmware) <> ''
        ) sub
        WHERE i.id = sub.printer_id
          AND sub.id IS NOT NULL;

        -- Segundo intento sin restringir modelo por si existe la versión bajo otro registro
        UPDATE public.impresoras i
        SET id_firmware = (
            SELECT f.id
            FROM public.firmwares f
            WHERE f.version = TRIM(i.version_firmware)
            ORDER BY f.created_at DESC
            LIMIT 1
        )
        WHERE i.id_firmware IS NULL
          AND i.version_firmware IS NOT NULL
          AND TRIM(i.version_firmware) <> '';

        ALTER TABLE public.impresoras
            DROP COLUMN version_firmware;
    END IF;
END $$;
