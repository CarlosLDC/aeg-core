-- V69: Conectar facturas_comerciales directamente a empresas (id_empresa).

ALTER TABLE public.facturas_comerciales
    ADD COLUMN IF NOT EXISTS id_empresa BIGINT REFERENCES public.empresas(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_facturas_comerciales_id_empresa
    ON public.facturas_comerciales(id_empresa);

-- Backfill desde id_distribuidora
UPDATE public.facturas_comerciales f
SET id_empresa = s.id_empresa
FROM public.distribuidoras d
JOIN public.sucursales s ON s.id = d.id_sucursal
WHERE f.id_distribuidora = d.id
  AND f.id_empresa IS NULL;

-- Backfill desde id_cliente
UPDATE public.facturas_comerciales f
SET id_empresa = s.id_empresa
FROM public.clientes c
JOIN public.sucursales s ON s.id = c.id_sucursal
WHERE f.id_cliente = c.id
  AND f.id_empresa IS NULL;

-- Backfill desde clientes_comerciales por razón social
UPDATE public.facturas_comerciales f
SET id_empresa = e.id
FROM public.clientes_comerciales cc
JOIN public.empresas e
  ON LOWER(TRIM(e.razon_social)) = LOWER(TRIM(cc.nombre))
WHERE f.id_cliente_comercial = cc.id
  AND f.id_empresa IS NULL;
