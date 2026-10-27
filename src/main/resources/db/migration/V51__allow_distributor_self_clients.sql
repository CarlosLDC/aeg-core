-- V51: Allow distributor branches to be clients of their own distributor,
-- creating self-client records so printers can be assigned to them.

INSERT INTO public.clientes (id_sucursal, id_distribuidora, created_at, review_status)
SELECT d.id_sucursal, d.id, NOW(), 'ACTIVE'
FROM public.distribuidoras d
WHERE NOT EXISTS (
    SELECT 1 FROM public.clientes c
    WHERE c.id_sucursal = d.id_sucursal AND c.id_distribuidora = d.id
);

UPDATE public.sucursales s
SET es_cliente = true
FROM public.distribuidoras d
WHERE s.id = d.id_sucursal;
