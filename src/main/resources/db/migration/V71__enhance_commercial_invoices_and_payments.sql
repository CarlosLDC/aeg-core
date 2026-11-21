-- V71: Monto total en facturas comerciales, metadatos en pagos comerciales y unificación de pagos históricos de crédito.

ALTER TABLE public.facturas_comerciales
    ADD COLUMN IF NOT EXISTS monto_total NUMERIC(12, 2) NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_facturas_comerciales_monto_total'
    ) THEN
        ALTER TABLE public.facturas_comerciales
            ADD CONSTRAINT chk_facturas_comerciales_monto_total
            CHECK (monto_total IS NULL OR monto_total >= 0);
    END IF;
END $$;

ALTER TABLE public.pagos_comerciales
    ADD COLUMN IF NOT EXISTS fecha_pago DATE,
    ADD COLUMN IF NOT EXISTS notas TEXT,
    ADD COLUMN IF NOT EXISTS registered_by BIGINT NULL REFERENCES public.users(id) ON DELETE SET NULL;

UPDATE public.pagos_comerciales
SET fecha_pago = (created_at AT TIME ZONE 'UTC')::date
WHERE fecha_pago IS NULL;

ALTER TABLE public.pagos_comerciales
    ALTER COLUMN fecha_pago SET DEFAULT CURRENT_DATE,
    ALTER COLUMN fecha_pago SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pagos_comerciales_factura_fecha
    ON public.pagos_comerciales (id_factura_comercial, fecha_pago DESC, id DESC);

INSERT INTO public.pagos_comerciales (
    id_factura_comercial,
    metodo_banco,
    referencia,
    monto,
    cuenta_destino,
    fecha_pago,
    notas,
    registered_by,
    created_at
)
SELECT
    i.id_factura_comercial,
    'OTRO',
    LEFT(p.referencia, 100),
    p.monto,
    NULL,
    p.fecha_pago,
    CASE
        WHEN p.notas IS NOT NULL AND BTRIM(p.notas) <> ''
            THEN CONCAT('[Migrado de crédito ', i.serial_fiscal, '] ', BTRIM(p.notas))
        ELSE CONCAT('[Migrado de crédito ', i.serial_fiscal, ']')
    END,
    p.registered_by,
    p.created_at
FROM public.impresora_pagos_credito p
INNER JOIN public.impresoras i ON i.id = p.id_impresora
WHERE i.id_factura_comercial IS NOT NULL;

DELETE FROM public.impresora_pagos_credito p
USING public.impresoras i
WHERE i.id = p.id_impresora
  AND i.id_factura_comercial IS NOT NULL;
