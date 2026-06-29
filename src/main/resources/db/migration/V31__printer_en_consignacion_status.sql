-- Impresoras asignadas a distribuidor sin pago: estatus explícito en consignación.
UPDATE public.impresoras
SET estatus = 'en_consignacion'
WHERE estatus = 'asignada'
  AND se_pago = false
  AND id_distribuidora IS NOT NULL;
