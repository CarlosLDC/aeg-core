ALTER TABLE public.precintos
    ADD COLUMN creation_batch_id UUID NULL;

ALTER TABLE public.impresoras
    ADD COLUMN creation_batch_id UUID NULL;

CREATE INDEX idx_precintos_creation_batch_id ON public.precintos (creation_batch_id)
    WHERE creation_batch_id IS NOT NULL;

CREATE INDEX idx_impresoras_creation_batch_id ON public.impresoras (creation_batch_id)
    WHERE creation_batch_id IS NOT NULL;
