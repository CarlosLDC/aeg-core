-- Índices sobre columnas FK / filtros frecuentes que no tenían índice.
CREATE INDEX IF NOT EXISTS idx_impresoras_id_cliente ON public.impresoras (id_cliente);
CREATE INDEX IF NOT EXISTS idx_impresoras_id_distribuidora ON public.impresoras (id_distribuidora);
CREATE INDEX IF NOT EXISTS idx_impresoras_id_modelo_impresora ON public.impresoras (id_modelo_impresora);
CREATE INDEX IF NOT EXISTS idx_impresoras_id_software ON public.impresoras (id_software);
CREATE INDEX IF NOT EXISTS idx_impresoras_status ON public.impresoras (estatus);
CREATE INDEX IF NOT EXISTS idx_precintos_id_impresora ON public.precintos (id_impresora);
CREATE INDEX IF NOT EXISTS idx_clientes_id_sucursal ON public.clientes (id_sucursal);
CREATE INDEX IF NOT EXISTS idx_distribuidoras_id_sucursal ON public.distribuidoras (id_sucursal);
CREATE INDEX IF NOT EXISTS idx_sucursales_id_empresa ON public.sucursales (id_empresa);
CREATE INDEX IF NOT EXISTS idx_contratos_distribuidoras_id_distribuidora
    ON public.contratos_distribuidoras (id_distribuidora);
CREATE INDEX IF NOT EXISTS idx_servicios_tecnicos_id_impresora ON public.servicios_tecnicos (id_impresora);
CREATE INDEX IF NOT EXISTS idx_inspecciones_anuales_id_impresora ON public.inspecciones_anuales (id_impresora);
