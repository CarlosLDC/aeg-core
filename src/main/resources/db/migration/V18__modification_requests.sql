CREATE TABLE IF NOT EXISTS public.modification_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    proposed_data JSONB NULL,
    requested_by BIGINT NOT NULL REFERENCES public.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_modification_requests_status
    ON public.modification_requests (status);

CREATE INDEX IF NOT EXISTS idx_modification_requests_employee_id
    ON public.modification_requests (employee_id);

CREATE INDEX IF NOT EXISTS idx_modification_requests_requested_by
    ON public.modification_requests (requested_by);
