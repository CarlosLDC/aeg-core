-- V66: Eliminar promotores de facturas comerciales
ALTER TABLE public.facturas_comerciales
    DROP COLUMN IF EXISTS id_promotor;

DROP TABLE IF EXISTS public.promotores_comerciales CASCADE;
