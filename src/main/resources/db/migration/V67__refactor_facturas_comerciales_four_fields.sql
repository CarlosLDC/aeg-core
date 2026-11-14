-- V67: Refactorización de facturas_comerciales a exactamente 4 campos obligatorios:
-- numero_factura, id_cliente_comercial, modalidad, estado_cobro.

-- 1. Asegurar que no existan valores nulos en numero_factura
UPDATE public.facturas_comerciales
SET numero_factura = 'FAC-' || LPAD(id::text, 6, '0')
WHERE numero_factura IS NULL OR TRIM(numero_factura) = '';

-- 2. Asegurar que no existan valores nulos en id_cliente_comercial
INSERT INTO public.clientes_comerciales (nombre)
VALUES ('Cliente General')
ON CONFLICT (nombre) DO NOTHING;

UPDATE public.facturas_comerciales
SET id_cliente_comercial = (
    SELECT id FROM public.clientes_comerciales WHERE nombre = 'Cliente General' LIMIT 1
)
WHERE id_cliente_comercial IS NULL;

-- 3. Eliminar columnas no pertenecientes al modelo simplificado
ALTER TABLE public.facturas_comerciales
    DROP COLUMN IF EXISTS sufijo_control,
    DROP COLUMN IF EXISTS notas_comerciales;

-- 4. Establecer restricciones NOT NULL en los 4 campos
ALTER TABLE public.facturas_comerciales
    ALTER COLUMN numero_factura SET NOT NULL,
    ALTER COLUMN id_cliente_comercial SET NOT NULL;
