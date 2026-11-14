-- V68: Reemplazar clientes_comerciales por distribuidor (distribuidoras) o cliente (clientes) en facturas_comerciales.

ALTER TABLE public.facturas_comerciales
    ADD COLUMN IF NOT EXISTS id_distribuidora BIGINT REFERENCES public.distribuidoras(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS id_cliente BIGINT REFERENCES public.clientes(id) ON DELETE SET NULL;

ALTER TABLE public.facturas_comerciales
    ALTER COLUMN id_cliente_comercial DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_facturas_comerciales_id_distribuidora
    ON public.facturas_comerciales(id_distribuidora);

CREATE INDEX IF NOT EXISTS idx_facturas_comerciales_id_cliente_fk
    ON public.facturas_comerciales(id_cliente);

-- Intentar vincular facturas existentes con distribuidoras por coincidencia de razón social
UPDATE public.facturas_comerciales f
SET id_distribuidora = d.id
FROM public.clientes_comerciales cc
JOIN public.empresas e
  ON LOWER(TRIM(e.razon_social)) = LOWER(TRIM(cc.nombre))
JOIN public.sucursales s
  ON s.id_empresa = e.id
JOIN public.distribuidoras d
  ON d.id_sucursal = s.id
WHERE f.id_cliente_comercial = cc.id
  AND f.id_distribuidora IS NULL
  AND f.id_cliente IS NULL;

-- Intentar vincular las restantes con clientes por coincidencia de razón social
UPDATE public.facturas_comerciales f
SET id_cliente = c.id
FROM public.clientes_comerciales cc
JOIN public.empresas e
  ON LOWER(TRIM(e.razon_social)) = LOWER(TRIM(cc.nombre))
JOIN public.sucursales s
  ON s.id_empresa = e.id
JOIN public.clientes c
  ON c.id_sucursal = s.id
WHERE f.id_cliente_comercial = cc.id
  AND f.id_distribuidora IS NULL
  AND f.id_cliente IS NULL;
