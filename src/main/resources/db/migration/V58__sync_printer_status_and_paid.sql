-- V58: Sincronizar estatus y se_pago para que estatus sea la única fuente de verdad del ciclo de vida.
-- 1. Crea tabla de respaldo public.impresoras_backup_v58 antes de cualquier modificación.
-- 2. Si están en 'asignada' y se_pago = false, pasan a 'en_consignacion'.
-- 3. Sincroniza se_pago con el estatus resultante ('asignada'/'enajenada' -> true; 'en_consignacion'/'sin_asignar'/'de_fabrica' -> false).

CREATE TABLE IF NOT EXISTS public.impresoras_backup_v58 AS
SELECT * FROM public.impresoras;

UPDATE public.impresoras
SET estatus = 'en_consignacion'
WHERE estatus = 'asignada'
  AND se_pago = false;

UPDATE public.impresoras
SET se_pago = false
WHERE estatus IN ('en_consignacion', 'sin_asignar', 'de_fabrica')
  AND se_pago = true;

UPDATE public.impresoras
SET se_pago = true
WHERE estatus IN ('asignada', 'enajenada')
  AND se_pago = false;
