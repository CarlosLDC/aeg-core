-- Unify brand + model into a single codigo_modelo value (e.g. AEG + R1 → AEG-R1).
-- Skip re-prefixing when codigo_modelo already starts with marca- or equals marca.

UPDATE public.modelos_impresora
SET codigo_modelo = CASE
  WHEN marca IS NULL OR TRIM(marca) = '' THEN TRIM(codigo_modelo)
  WHEN UPPER(TRIM(codigo_modelo)) = UPPER(TRIM(marca)) THEN TRIM(codigo_modelo)
  WHEN UPPER(TRIM(codigo_modelo)) LIKE UPPER(TRIM(marca)) || '-%' THEN TRIM(codigo_modelo)
  ELSE TRIM(marca) || '-' || TRIM(codigo_modelo)
END;

ALTER TABLE public.modelos_impresora
  DROP COLUMN marca;
