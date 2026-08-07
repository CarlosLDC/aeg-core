-- Enforce unique printer MAC addresses (case/colon-insensitive).
-- Multiple NULL or blank MACs remain allowed (impresoras aún no fiscalizadas / de fábrica).

DO $$
DECLARE
  dup_count integer;
BEGIN
  SELECT COUNT(*) INTO dup_count
  FROM (
    SELECT REPLACE(UPPER(TRIM(direccion_mac)), ':', '') AS compact_mac
    FROM public.impresoras
    WHERE direccion_mac IS NOT NULL
      AND TRIM(direccion_mac) <> ''
    GROUP BY REPLACE(UPPER(TRIM(direccion_mac)), ':', '')
    HAVING COUNT(*) > 1
  ) dups;

  IF dup_count > 0 THEN
    RAISE EXCEPTION
      'Cannot create unique MAC index: % duplicate compact MAC group(s) in impresoras. Resolve duplicates before migrating.',
      dup_count;
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS impresoras_direccion_mac_compact_uq
  ON public.impresoras (REPLACE(UPPER(TRIM(direccion_mac)), ':', ''))
  WHERE direccion_mac IS NOT NULL
    AND TRIM(direccion_mac) <> '';
