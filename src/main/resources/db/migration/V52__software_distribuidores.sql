-- V52: Distribuidores autorizados de cada programa de computación (Art. 46 h SENIAT)
CREATE TABLE IF NOT EXISTS public.software_distribuidores (
    software_id BIGINT NOT NULL REFERENCES public.software (id) ON DELETE CASCADE,
    id_empresa  BIGINT NOT NULL REFERENCES public.empresas (id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_software_distribuidores PRIMARY KEY (software_id, id_empresa)
);

CREATE INDEX IF NOT EXISTS idx_software_distribuidores_software ON public.software_distribuidores (software_id);
CREATE INDEX IF NOT EXISTS idx_software_distribuidores_empresa ON public.software_distribuidores (id_empresa);
