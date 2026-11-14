-- V65: Normalización relacional (3FN) de facturas_comerciales
-- Extrae clientes comerciales y promotores/vendedores a tablas catálogo con FK.

CREATE TABLE IF NOT EXISTS public.clientes_comerciales (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(120) NOT NULL UNIQUE,
    rif        VARCHAR(20),
    notas      VARCHAR(255),
    activo     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.promotores_comerciales (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL UNIQUE,
    activo     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE public.facturas_comerciales
    ADD COLUMN IF NOT EXISTS id_cliente_comercial BIGINT REFERENCES public.clientes_comerciales(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS id_promotor          BIGINT REFERENCES public.promotores_comerciales(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_facturas_comerciales_id_cliente
    ON public.facturas_comerciales(id_cliente_comercial);

CREATE INDEX IF NOT EXISTS idx_facturas_comerciales_id_promotor
    ON public.facturas_comerciales(id_promotor);

-- 1. Migrar promotores/vendedores existentes
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'facturas_comerciales'
          AND column_name = 'promotor_vendedor'
    ) THEN
        INSERT INTO public.promotores_comerciales (nombre)
        SELECT DISTINCT TRIM(promotor_vendedor)
        FROM public.facturas_comerciales
        WHERE promotor_vendedor IS NOT NULL
          AND TRIM(promotor_vendedor) <> ''
        ON CONFLICT (nombre) DO NOTHING;

        UPDATE public.facturas_comerciales f
        SET id_promotor = p.id
        FROM public.promotores_comerciales p
        WHERE f.promotor_vendedor IS NOT NULL
          AND TRIM(f.promotor_vendedor) = p.nombre
          AND f.id_promotor IS NULL;
    END IF;
END $$;

-- 2. Migrar factura_cliente si existiera algún valor no nulo
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'facturas_comerciales'
          AND column_name = 'factura_cliente'
    ) THEN
        INSERT INTO public.clientes_comerciales (nombre)
        SELECT DISTINCT TRIM(factura_cliente)
        FROM public.facturas_comerciales
        WHERE factura_cliente IS NOT NULL
          AND TRIM(factura_cliente) <> ''
        ON CONFLICT (nombre) DO NOTHING;

        UPDATE public.facturas_comerciales f
        SET id_cliente_comercial = c.id
        FROM public.clientes_comerciales c
        WHERE f.factura_cliente IS NOT NULL
          AND TRIM(f.factura_cliente) = c.nombre
          AND f.id_cliente_comercial IS NULL;
    END IF;
END $$;

-- 3. Extraer clientes comerciales embebidos en notas_comerciales (datos semilla V64)
DO $$
DECLARE
    v_map RECORD;
    v_cliente_id BIGINT;
BEGIN
    FOR v_map IN
        SELECT * FROM (VALUES
            ('Cliente: Chino San Félix / Edgar', 'Chino San Félix / Edgar', NULL),
            ('Comarcas', 'Comarcas', NULL),
            ('Comarcas (Binance)', 'Comarcas', 'Binance'),
            ('Comarcas - Tienen Deuda', 'Comarcas', 'Tienen Deuda'),
            ('Yaracuy', 'Yaracuy', NULL),
            ('Kodex', 'Kodex', NULL),
            ('Kodex (sufijo X en serial 353)', 'Kodex', 'Sufijo X en serial 353'),
            ('Kodex - Consignación sin factura propia', 'Kodex', 'Consignación sin factura propia'),
            ('Chino en el Core 8', 'Chino en el Core 8', NULL),
            ('Cliente: Traki', 'Traki', NULL),
            ('Cliente: Costa America', 'Costa America', NULL),
            ('D3XD', 'D3XD', NULL),
            ('STC', 'STC', NULL),
            ('STC (incluye pago USD 600.00 en serial 155 y consignación en serial 175)', 'STC', 'Incluye pago USD 600.00 en serial 155 y consignación en serial 175'),
            ('STC - Debe el IVA', 'STC', 'Debe el IVA'),
            ('STC - DEBE EL IVA de 2 Maquinas a 500', 'STC', 'DEBE EL IVA de 2 Maquinas a 500'),
            ('Cliente: SKY', 'SKY', NULL),
            ('El Castillo - Pago BNC sin número de factura', 'El Castillo', 'Pago BNC sin número de factura'),
            ('Prointel - El Castillo - Pago BNC sin número de factura', 'Prointel - El Castillo', 'Pago BNC sin número de factura'),
            ('Prointel - El Castillo - Consignación sin factura propia', 'Prointel - El Castillo', 'Consignación sin factura propia'),
            ('Kinpe', 'Kinpe', NULL),
            ('Multi Equipos', 'Multi Equipos', NULL),
            ('Multi Equipos (serial 50: CUENTA EDGAR)', 'Multi Equipos', 'Serial 50: CUENTA EDGAR'),
            ('Multi Equipos - Pago Zelle sin número de factura', 'Multi Equipos', 'Pago Zelle sin número de factura'),
            ('Asor Services - Pago Zelle sin número de factura', 'Asor Services', 'Pago Zelle sin número de factura'),
            ('Cliente: Cauchera Orinoco', 'Cauchera Orinoco', NULL),
            ('Cliente: Totos', 'Totos', NULL),
            ('AEG - Debe 200 AUN DE LA MAQUINA', 'AEG', 'Debe 200 AUN DE LA MAQUINA'),
            ('Gesco - Llevar la factura cobrar las retenciones', 'Gesco', 'Llevar la factura cobrar las retenciones'),
            ('HERMANO BLADIMIR SE DEBE FACTURAR', 'Hermano Bladimir', 'SE DEBE FACTURAR'),
            ('Vendida a MAX SE DEBE FACTURAR', 'MAX', 'SE DEBE FACTURAR'),
            ('REGALO WILLY', 'Willy', 'REGALO WILLY'),
            ('Regalada a la señora Rocio', 'Señora Rocio', 'Regalada')
        ) AS t(nota_original, cliente_nombre, nota_limpia)
    LOOP
        INSERT INTO public.clientes_comerciales (nombre)
        VALUES (v_map.cliente_nombre)
        ON CONFLICT (nombre) DO NOTHING;

        SELECT id INTO v_cliente_id
        FROM public.clientes_comerciales
        WHERE nombre = v_map.cliente_nombre;

        UPDATE public.facturas_comerciales
        SET id_cliente_comercial = COALESCE(id_cliente_comercial, v_cliente_id),
            notas_comerciales = v_map.nota_limpia
        WHERE TRIM(notas_comerciales) = v_map.nota_original;
    END LOOP;
END $$;

-- 4. Eliminar columnas desnormalizadas
ALTER TABLE public.facturas_comerciales
    DROP COLUMN IF EXISTS factura_cliente,
    DROP COLUMN IF EXISTS promotor_vendedor;
