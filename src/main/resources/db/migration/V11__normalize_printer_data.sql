-- Migración para normalizar los valores de estatus y tipo_dispositivo en la tabla impresoras y otras.
-- Esto asegura que los valores coincidan con los Enums de Java y evita errores de AttributeConverter.

-- Normalizar impresoras
UPDATE public.impresoras 
SET estatus = LOWER(TRIM(estatus)),
    tipo_dispositivo = LOWER(TRIM(tipo_dispositivo));

UPDATE public.impresoras SET estatus = 'activo' WHERE estatus IN ('active', 'enabled', 'on');
UPDATE public.impresoras SET estatus = 'inactivo' WHERE estatus IN ('inactive', 'disabled', 'off');
UPDATE public.impresoras SET tipo_dispositivo = 'interno' WHERE tipo_dispositivo = 'internal';
UPDATE public.impresoras SET tipo_dispositivo = 'externo' WHERE tipo_dispositivo = 'external';

-- Normalizar empresas
UPDATE public.empresas 
SET tipo_contribuyente = LOWER(TRIM(tipo_contribuyente)) 
WHERE tipo_contribuyente IS NOT NULL;

-- Normalizar precintos (seals)
UPDATE public.precintos 
SET color = LOWER(TRIM(color)), 
    estatus = LOWER(TRIM(estatus));

UPDATE public.precintos SET estatus = 'disponible' WHERE estatus IN ('available', 'free');
UPDATE public.precintos SET estatus = 'en_impresora' WHERE estatus IN ('installed', 'used');

-- Normalizar empleados
UPDATE public.empleados
SET tipo = LOWER(TRIM(tipo))
WHERE tipo IS NOT NULL;
