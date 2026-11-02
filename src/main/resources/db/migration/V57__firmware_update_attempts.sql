-- Seguimiento de actualizaciones de firmware lanzadas desde Tools y bandera de versión sin verificar.

ALTER TABLE public.impresoras
    ADD COLUMN IF NOT EXISTS firmware_update_unverified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS public.firmware_update_attempts (
    id                BIGSERIAL PRIMARY KEY,
    id_impresora      BIGINT      NOT NULL REFERENCES public.impresoras (id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL,
    previous_version  VARCHAR(50),
    requested_at      TIMESTAMPTZ NOT NULL,
    next_check_at     TIMESTAMPTZ NOT NULL,
    attempts          INTEGER     NOT NULL DEFAULT 0,
    got_reply         BOOLEAN     NOT NULL DEFAULT FALSE,
    finished_at       TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fw_update_attempts_due
    ON public.firmware_update_attempts (status, next_check_at);
CREATE INDEX IF NOT EXISTS idx_fw_update_attempts_printer
    ON public.firmware_update_attempts (id_impresora, status);
