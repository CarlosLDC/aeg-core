-- V60: Autorizacion previa para enajenar impresoras en consignacion, cuotas de credito y registro de pagos.

ALTER TABLE public.impresoras
    ADD COLUMN IF NOT EXISTS enajenacion_autorizada BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS enajenacion_autorizada_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS enajenacion_autorizada_by BIGINT NULL REFERENCES public.users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS cuotas_credito INTEGER NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_impresoras_cuotas_credito'
    ) THEN
        ALTER TABLE public.impresoras
            ADD CONSTRAINT chk_impresoras_cuotas_credito
            CHECK (cuotas_credito IS NULL OR cuotas_credito > 0);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.impresora_pagos_credito (
    id            BIGSERIAL PRIMARY KEY,
    id_impresora  BIGINT NOT NULL REFERENCES public.impresoras (id) ON DELETE CASCADE,
    monto         NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    fecha_pago    DATE NOT NULL,
    referencia    VARCHAR(120) NULL,
    notas         TEXT NULL,
    registered_by BIGINT NULL REFERENCES public.users (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_impresora_pagos_credito_impresora_fecha
    ON public.impresora_pagos_credito (id_impresora, fecha_pago DESC, id DESC);
