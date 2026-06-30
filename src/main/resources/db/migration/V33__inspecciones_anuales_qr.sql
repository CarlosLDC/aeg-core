ALTER TABLE public.inspecciones_anuales
    ADD COLUMN mqtt_qr_codigo text,
    ADD COLUMN mqtt_qr_registro text,
    ADD COLUMN mqtt_qr_mac text,
    ADD COLUMN mqtt_qr_fecha text;
