alter table public.inspecciones_anuales
  drop column if exists url_fotos;

alter table public.servicios_tecnicos
  drop column if exists url_fotos;
