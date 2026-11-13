-- V59: Agrega estatus 'a_credito' para impresoras enajenadas desde 'en_consignacion'.

ALTER TABLE public.impresoras
    DROP CONSTRAINT IF EXISTS chk_impresoras_estatus;

ALTER TABLE public.impresoras
    ADD CONSTRAINT chk_impresoras_estatus CHECK (estatus IN
        ('de_fabrica', 'sin_asignar', 'asignada', 'en_consignacion', 'enajenada', 'a_credito', 'desincorporada', 'laboratorio'));
