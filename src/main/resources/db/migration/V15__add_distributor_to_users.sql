-- V15: Add optional distributor reference to users
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS distributor_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'users_distributor_id_fkey'
    ) THEN
        ALTER TABLE public.users
            ADD CONSTRAINT users_distributor_id_fkey
            FOREIGN KEY (distributor_id)
            REFERENCES public.distribuidoras(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_distributor_id
    ON public.users(distributor_id);
