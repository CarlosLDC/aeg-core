ALTER TABLE public.distribuidoras
    ADD COLUMN IF NOT EXISTS puede_inspeccion_anual boolean NOT NULL DEFAULT true;

COMMENT ON COLUMN public.distribuidoras.puede_inspeccion_anual IS
    'Si false, usuarios de esta distribuidora no pueden crear/editar inspecciones anuales.';
