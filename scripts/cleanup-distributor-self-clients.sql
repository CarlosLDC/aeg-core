-- Manual cleanup: distributor branches incorrectly registered as their own clients.
-- Prefer deploying aeg-core so Flyway runs V36__remove_distributor_self_clients.sql automatically.
-- Use this script only for ad-hoc repair against production/staging without a deploy.

BEGIN;

-- Preview rows that will be removed:
-- SELECT c.id, c.id_sucursal, c.id_distribuidora, d.id_sucursal AS distributor_branch_id
-- FROM public.clientes c
-- INNER JOIN public.distribuidoras d ON d.id = c.id_distribuidora
-- WHERE c.id_sucursal = d.id_sucursal;

UPDATE public.impresoras i
SET id_cliente = NULL
FROM public.clientes c
         INNER JOIN public.distribuidoras d ON d.id = c.id_distribuidora
WHERE i.id_cliente = c.id
  AND c.id_sucursal = d.id_sucursal;

DELETE
FROM public.modification_requests mr
    USING public.clientes c
        INNER JOIN public.distribuidoras d ON d.id = c.id_distribuidora
WHERE mr.target_type = 'CLIENT'
  AND mr.target_id = c.id
  AND c.id_sucursal = d.id_sucursal;

DELETE
FROM public.clientes c
    USING public.distribuidoras d
WHERE c.id_distribuidora = d.id
  AND c.id_sucursal = d.id_sucursal;

UPDATE public.sucursales s
SET es_cliente = false
WHERE COALESCE(s.es_cliente, false) = true
  AND NOT EXISTS (
    SELECT 1
    FROM public.clientes c
    WHERE c.id_sucursal = s.id
);

COMMIT;
