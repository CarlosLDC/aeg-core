ALTER TABLE public.empleados
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(20);

UPDATE public.empleados
SET review_status = 'ACTIVE'
WHERE review_status IS NULL;

ALTER TABLE public.empleados
    ALTER COLUMN review_status SET NOT NULL;

ALTER TABLE public.empleados
    ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE public.empleados
SET version = 0
WHERE version IS NULL;

ALTER TABLE public.empleados
    ALTER COLUMN version SET NOT NULL;
