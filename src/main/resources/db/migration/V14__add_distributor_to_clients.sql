-- V14: Add optional distributor reference to clients
ALTER TABLE public.clientes
    ADD COLUMN IF NOT EXISTS id_distribuidora BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'clientes_id_distribuidora_fkey'
    ) THEN
        ALTER TABLE public.clientes
            ADD CONSTRAINT clientes_id_distribuidora_fkey
            FOREIGN KEY (id_distribuidora)
            REFERENCES public.distribuidoras(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_clientes_id_distribuidora
    ON public.clientes(id_distribuidora);
