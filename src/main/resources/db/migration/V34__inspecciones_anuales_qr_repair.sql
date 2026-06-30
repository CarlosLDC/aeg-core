ALTER TABLE public.inspecciones_anuales
    ADD COLUMN IF NOT EXISTS mqtt_qr_codigo text,
    ADD COLUMN IF NOT EXISTS mqtt_qr_registro text,
    ADD COLUMN IF NOT EXISTS mqtt_qr_mac text,
    ADD COLUMN IF NOT EXISTS mqtt_qr_fecha text;
