-- Enable AEG (the factory company) to operate as a distributor.
-- Insert a distributor record for the primary branch of the factory company if none exists.
INSERT INTO public.distribuidoras (id_sucursal, created_at, puede_inspeccion_anual)
SELECT s.id, NOW(), true
FROM public.empresas e
JOIN public.sucursales s ON s.id_empresa = e.id
WHERE e.organization_type = 'FACTORY'
  AND NOT EXISTS (
      SELECT 1 FROM public.distribuidoras d WHERE d.id_sucursal = s.id
  )
ORDER BY s.id
LIMIT 1;

-- Mark the factory branch as DISTRIBUTOR
UPDATE public.sucursales s
SET organization_role = 'DISTRIBUTOR',
    es_distribuidora = true
WHERE s.id IN (
    SELECT d.id_sucursal
    FROM public.distribuidoras d
    JOIN public.sucursales s2 ON s2.id = d.id_sucursal
    JOIN public.empresas e ON e.id = s2.id_empresa
    WHERE e.organization_type = 'FACTORY'
);
