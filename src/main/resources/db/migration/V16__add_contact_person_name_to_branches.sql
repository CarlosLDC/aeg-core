ALTER TABLE public.sucursales
  ADD COLUMN IF NOT EXISTS nombre_persona_contacto varchar(255);
