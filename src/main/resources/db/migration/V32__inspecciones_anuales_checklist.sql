alter table public.inspecciones_anuales
  add column if not exists chk_precinto boolean,
  add column if not exists chk_etiqueta_fiscal boolean,
  add column if not exists chk_factura boolean,
  add column if not exists chk_nota_credito boolean,
  add column if not exists chk_sensor_papel boolean;

update public.inspecciones_anuales
  set chk_precinto = not precinto_violentado
  where chk_precinto is null;
