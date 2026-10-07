-- Migration V46: Generar llave de encriptacion para impresoras que tienen serial_fiscal y direccion_mac
-- Algoritmo equivalente a firmware ESP32: md5(serial_fiscal || '|' || direccion_mac_colon).substring(0, 16)

UPDATE public.impresoras
SET llave_encriptacion = substr(
    md5(
        upper(trim(serial_fiscal)) || '|' ||
        CASE
            WHEN upper(trim(direccion_mac)) ~ '^([0-9A-F]{2}:){5}[0-9A-F]{2}$'
                THEN upper(trim(direccion_mac))
            WHEN upper(trim(direccion_mac)) ~ '^[0-9A-F]{12}$'
                THEN upper(regexp_replace(upper(trim(direccion_mac)), '([0-9A-F]{2})(?=[0-9A-F])', '\1:', 'g'))
            ELSE upper(trim(direccion_mac))
        END
    ),
    1,
    16
)
WHERE llave_encriptacion IS NULL
  AND serial_fiscal IS NOT NULL
  AND trim(serial_fiscal) <> ''
  AND direccion_mac IS NOT NULL
  AND trim(direccion_mac) <> '';
