-- V53: Monitoreo y persistencia indefinida de logs MQTT de impresoras (DebugMQTT)
-- Estrategia de alto rendimiento:
-- 1. Tabla particionada por rango de fecha (recorded_at) para evitar saturación de buffer pool.
-- 2. Permite poda de particiones en consultas por fecha y hora (partition pruning).
-- 3. Permite borrado ultra-rápido por fecha mediante DROP TABLE de particiones completas o DELETE por lote.

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs (
    id BIGSERIAL NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    printer_id BIGINT NULL REFERENCES public.impresoras (id) ON DELETE SET NULL,
    mac VARCHAR(12) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    PRIMARY KEY (id, recorded_at)
) PARTITION BY RANGE (recorded_at);

-- Partición por defecto para capturar cualquier registro que caiga fuera de los rangos explícitos
CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_default
    PARTITION OF public.printer_mqtt_logs DEFAULT;

-- Particiones mensuales pre-creadas (2026 y 2027)
CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m01
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-01-01 00:00:00+00') TO ('2026-02-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m02
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-02-01 00:00:00+00') TO ('2026-03-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m03
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-03-01 00:00:00+00') TO ('2026-04-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m04
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-04-01 00:00:00+00') TO ('2026-05-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m05
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-05-01 00:00:00+00') TO ('2026-06-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m06
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-06-01 00:00:00+00') TO ('2026-07-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m07
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m08
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-08-01 00:00:00+00') TO ('2026-09-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m09
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-09-01 00:00:00+00') TO ('2026-10-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m10
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m11
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2026m12
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2026-12-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2027m01
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-02-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2027m02
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2027-02-01 00:00:00+00') TO ('2027-03-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2027m03
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2027-03-01 00:00:00+00') TO ('2027-04-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2027m04
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2027-04-01 00:00:00+00') TO ('2027-05-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2027m05
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2027-05-01 00:00:00+00') TO ('2027-06-01 00:00:00+00');

CREATE TABLE IF NOT EXISTS public.printer_mqtt_logs_y2027m06
    PARTITION OF public.printer_mqtt_logs
    FOR VALUES FROM ('2027-06-01 00:00:00+00') TO ('2027-07-01 00:00:00+00');

-- Índices en la tabla particionada (Postgres propaga automáticamente a cada partición)
CREATE INDEX IF NOT EXISTS idx_printer_mqtt_logs_printer_recorded_at
    ON public.printer_mqtt_logs (printer_id, recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_printer_mqtt_logs_mac_recorded_at
    ON public.printer_mqtt_logs (mac, recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_printer_mqtt_logs_recorded_at
    ON public.printer_mqtt_logs (recorded_at DESC);
