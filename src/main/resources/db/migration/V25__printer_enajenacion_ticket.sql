ALTER TABLE public.impresoras
    ADD COLUMN header JSONB NULL,
    ADD COLUMN trailer JSONB NULL;
