alter table public.modelos_impresora
  add column if not exists prefijo_serial varchar(3);

update public.modelos_impresora
set prefijo_serial = 'GRA'
where upper(trim(codigo_modelo)) = 'AEG-R1'
  and (prefijo_serial is null or trim(prefijo_serial) = '');

create unique index if not exists uq_modelos_impresora_prefijo_serial
  on public.modelos_impresora (upper(prefijo_serial))
  where prefijo_serial is not null;
