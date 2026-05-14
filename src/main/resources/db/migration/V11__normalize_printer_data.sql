-- Migración para normalizar los valores de estatus y tipo_dispositivo en la tabla impresoras.
-- Esto asegura que los valores coincidan con los Enums de Java y evita errores de AttributeConverter.

UPDATE public.impresoras 
SET estatus = LOWER(TRIM(estatus));

UPDATE public.impresoras 
SET tipo_dispositivo = LOWER(TRIM(tipo_dispositivo));

-- Asegurar que no haya valores inválidos (opcional: podrías poner un default o fallar)
-- Aquí simplemente nos aseguramos de que los valores conocidos estén bien.
-- Si hay un valor como 'active' en lugar de 'activo', podríamos mapearlo si quisiéramos.
UPDATE public.impresoras SET estatus = 'activo' WHERE estatus IN ('active', 'enabled');
UPDATE public.impresoras SET estatus = 'inactivo' WHERE estatus IN ('inactive', 'disabled');
UPDATE public.impresoras SET tipo_dispositivo = 'interno' WHERE tipo_dispositivo = 'internal';
UPDATE public.impresoras SET tipo_dispositivo = 'externo' WHERE tipo_dispositivo = 'external';

UPDATE public.empresas SET contributor_type = LOWER(TRIM(contributor_type)) WHERE contributor_type IS NOT NULL;
UPDATE public.precintos SET color = LOWER(TRIM(color)), estatus = LOWER(TRIM(estatus));

