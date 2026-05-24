ALTER TABLE public.clientes
    ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
