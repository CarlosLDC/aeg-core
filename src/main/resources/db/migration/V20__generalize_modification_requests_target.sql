ALTER TABLE public.modification_requests
    RENAME COLUMN employee_id TO target_id;

ALTER TABLE public.modification_requests
    ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE';

DROP INDEX IF EXISTS public.idx_modification_requests_employee_id;

CREATE INDEX IF NOT EXISTS idx_modification_requests_target_type_status
    ON public.modification_requests (target_type, status);

CREATE INDEX IF NOT EXISTS idx_modification_requests_target_type_target_id
    ON public.modification_requests (target_type, target_id);
