-- V47: Clients whose branch contact email is 'stcposca@gmail.com' belong to the
-- distributor "STC POS, C.A.". Reassign those clients and their printers to it.
-- Idempotent: safe to re-run.

DO $$
DECLARE
    v_stc_distributor_id BIGINT;
    v_clients_updated    INTEGER;
    v_printers_updated   INTEGER;
BEGIN
    SELECT d.id
    INTO v_stc_distributor_id
    FROM public.distribuidoras d
             JOIN public.sucursales s ON s.id = d.id_sucursal
             JOIN public.empresas e ON e.id = s.id_empresa
    WHERE regexp_replace(upper(e.razon_social), '[^A-Z]', '', 'g') = 'STCPOSCA'
    ORDER BY d.id
    LIMIT 1;

    IF v_stc_distributor_id IS NULL THEN
        RAISE NOTICE 'V47: distributor "STC POS, C.A." not found; nothing to do.';
        RETURN;
    END IF;

    -- Clients (excluding STC's own branches, see V36)
    UPDATE public.clientes c
    SET id_distribuidora = v_stc_distributor_id
    FROM public.sucursales s
    WHERE s.id = c.id_sucursal
      AND lower(trim(s.correo)) = 'stcposca@gmail.com'
      AND c.id_distribuidora IS DISTINCT FROM v_stc_distributor_id
      AND s.id_empresa <> (SELECT s2.id_empresa
                           FROM public.distribuidoras d2
                                    JOIN public.sucursales s2 ON s2.id = d2.id_sucursal
                           WHERE d2.id = v_stc_distributor_id);
    GET DIAGNOSTICS v_clients_updated = ROW_COUNT;

    -- Printers of every client now assigned to STC via the contact email
    UPDATE public.impresoras i
    SET id_distribuidora = v_stc_distributor_id
    FROM public.clientes c
             JOIN public.sucursales s ON s.id = c.id_sucursal
    WHERE i.id_cliente = c.id
      AND c.id_distribuidora = v_stc_distributor_id
      AND lower(trim(s.correo)) = 'stcposca@gmail.com'
      AND i.id_distribuidora IS DISTINCT FROM v_stc_distributor_id;
    GET DIAGNOSTICS v_printers_updated = ROW_COUNT;

    RAISE NOTICE 'V47: distributor id=%, clients updated=%, printers updated=%',
        v_stc_distributor_id, v_clients_updated, v_printers_updated;
END $$;
