-- Idempotent: safe to re-run after a failed deploy or manual drift.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'modification_requests'
          AND column_name = 'employee_id'
    ) THEN
        ALTER TABLE public.modification_requests
            RENAME COLUMN employee_id TO target_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'modification_requests'
          AND column_name = 'target_type'
    ) THEN
        ALTER TABLE public.modification_requests
            ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE';
    END IF;
END $$;

DROP INDEX IF EXISTS public.idx_modification_requests_employee_id;

CREATE INDEX IF NOT EXISTS idx_modification_requests_target_type_status
    ON public.modification_requests (target_type, status);

CREATE INDEX IF NOT EXISTS idx_modification_requests_target_type_target_id
    ON public.modification_requests (target_type, target_id);
