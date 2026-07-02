-- Remove invalid client rows where a distributor branch was linked as its own client.
-- Safe to re-run: only deletes self-referential distributor/client pairs.

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
