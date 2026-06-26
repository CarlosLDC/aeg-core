alter table public.inspecciones_anuales
  add column if not exists mqtt_registro_impresora text null,
  add column if not exists mqtt_set_date_rev_o_at bigint null,
  add column if not exists mqtt_numero_factura_prueba integer null;
