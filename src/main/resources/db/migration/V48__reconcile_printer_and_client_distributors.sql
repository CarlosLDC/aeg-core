-- V48: Reconcile printer and client distributor assignments from Mongo export.
-- 1. Creates backup tables (public.*_backup_v48) before any modification.
-- 2. Creates missing distributor companies (J-506743701 Adminsoft Solutions, C.A. and J-502564772 Edgar Tortolero).
-- 3. Updates public.impresoras.id_distribuidora for all 628 mapped printers and transitions 'sin_asignar' -> 'asignada'.
-- 4. Synchronizes public.clientes.id_distribuidora for clients linked to those printers (respecting V36 self-client rule).
-- Idempotent and safe to re-run.

-- ============================================================================
-- 1. BACKUP TABLES (Created only if they do not exist yet, preserving pre-V48 state)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.empresas_backup_v48 AS
SELECT * FROM public.empresas;

CREATE TABLE IF NOT EXISTS public.sucursales_backup_v48 AS
SELECT * FROM public.sucursales;

CREATE TABLE IF NOT EXISTS public.distribuidoras_backup_v48 AS
SELECT * FROM public.distribuidoras;

CREATE TABLE IF NOT EXISTS public.clientes_backup_v48 AS
SELECT * FROM public.clientes;

CREATE TABLE IF NOT EXISTS public.impresoras_backup_v48 AS
SELECT * FROM public.impresoras;

-- ============================================================================
-- 2. ENSURE MISSING DISTRIBUTOR COMPANIES, BRANCHES & DISTRIBUTORS EXIST
-- ============================================================================
DO $$
DECLARE
    v_emp_id  BIGINT;
    v_suc_id  BIGINT;
    v_dist_id BIGINT;
BEGIN
    -- 2A. ADMINSOFT SOLUTIONS, C.A. (J-506743701)
    SELECT id INTO v_emp_id
    FROM public.empresas
    WHERE regexp_replace(upper(rif), '[^A-Z0-9]', '', 'g') = 'J506743701'
    ORDER BY id
    LIMIT 1;

    IF v_emp_id IS NULL THEN
        INSERT INTO public.empresas (razon_social, created_at, rif, tipo_contribuyente, organization_type)
        VALUES ('Adminsoft Solutions, C.A.', NOW(), 'J-506743701', 'ordinario', 'STANDARD')
        RETURNING id INTO v_emp_id;
    END IF;

    SELECT id INTO v_suc_id
    FROM public.sucursales
    WHERE id_empresa = v_emp_id
    ORDER BY id
    LIMIT 1;

    IF v_suc_id IS NULL THEN
        INSERT INTO public.sucursales (
            id_empresa, ciudad, estado, direccion, telefono, correo,
            created_at, es_cliente, es_distribuidora, es_centro_servicio,
            nombre_persona_contacto, organization_role
        )
        VALUES (
            v_emp_id,
            'Valencia',
            'Carabobo',
            'AV RIO ORINOCO, RESIDENCIA CHELSEA COURT EDIF TORRE SUR PISO 10 APT 1003 URB EL PARRAL VALENCIA CARABOBO ZONA POSTAL 2001',
            '0412-5594001',
            NULL,
            NOW(),
            false,
            true,
            false,
            'Adminsoft Solutions, C.A.',
            'DISTRIBUTOR'
        )
        RETURNING id INTO v_suc_id;
    ELSE
        UPDATE public.sucursales
        SET es_distribuidora = true,
            organization_role = CASE
                WHEN organization_role = 'SERVICE_CENTER' THEN 'BOTH'
                WHEN organization_role = 'NONE' THEN 'DISTRIBUTOR'
                ELSE organization_role
            END
        WHERE id = v_suc_id;
    END IF;

    SELECT id INTO v_dist_id
    FROM public.distribuidoras
    WHERE id_sucursal = v_suc_id
    ORDER BY id
    LIMIT 1;

    IF v_dist_id IS NULL THEN
        INSERT INTO public.distribuidoras (id_sucursal, created_at, puede_inspeccion_anual)
        VALUES (v_suc_id, NOW(), true);
    END IF;

    -- 2B. EDGAR TORTOLERO / ETECHNOLOGY (J-502564772)
    v_emp_id  := NULL;
    v_suc_id  := NULL;
    v_dist_id := NULL;

    SELECT id INTO v_emp_id
    FROM public.empresas
    WHERE regexp_replace(upper(rif), '[^A-Z0-9]', '', 'g') = 'J502564772'
    ORDER BY id
    LIMIT 1;

    IF v_emp_id IS NULL THEN
        INSERT INTO public.empresas (razon_social, created_at, rif, tipo_contribuyente, organization_type)
        VALUES ('Edgar Tortolero', NOW(), 'J-502564772', 'ordinario', 'STANDARD')
        RETURNING id INTO v_emp_id;
    END IF;

    SELECT id INTO v_suc_id
    FROM public.sucursales
    WHERE id_empresa = v_emp_id
    ORDER BY id
    LIMIT 1;

    IF v_suc_id IS NULL THEN
        INSERT INTO public.sucursales (
            id_empresa, ciudad, estado, direccion, telefono, correo,
            created_at, es_cliente, es_distribuidora, es_centro_servicio,
            nombre_persona_contacto, organization_role
        )
        VALUES (
            v_emp_id,
            'Valencia',
            'Carabobo',
            'n/d',
            NULL,
            'etechnologypc@gmail.com',
            NOW(),
            false,
            true,
            false,
            'Edgar Tortolero',
            'DISTRIBUTOR'
        )
        RETURNING id INTO v_suc_id;
    ELSE
        UPDATE public.sucursales
        SET es_distribuidora = true,
            organization_role = CASE
                WHEN organization_role = 'SERVICE_CENTER' THEN 'BOTH'
                WHEN organization_role = 'NONE' THEN 'DISTRIBUTOR'
                ELSE organization_role
            END
        WHERE id = v_suc_id;
    END IF;

    SELECT id INTO v_dist_id
    FROM public.distribuidoras
    WHERE id_sucursal = v_suc_id
    ORDER BY id
    LIMIT 1;

    IF v_dist_id IS NULL THEN
        INSERT INTO public.distribuidoras (id_sucursal, created_at, puede_inspeccion_anual)
        VALUES (v_suc_id, NOW(), true);
    END IF;
END $$;

-- ============================================================================
-- 3. RECONCILE PRINTERS & CLIENTS FROM MONGO MAPPING
-- ============================================================================
DO $$
DECLARE
    v_printers_dist_updated   INTEGER := 0;
    v_printers_status_updated INTEGER := 0;
    v_clients_dist_updated    INTEGER := 0;
    v_unmatched_rifs          INTEGER := 0;
BEGIN
    CREATE TEMP TABLE tmp_printer_distributor_map (
        serial_fiscal   TEXT PRIMARY KEY,
        distributor_rif TEXT NOT NULL
    ) ON COMMIT DROP;

    INSERT INTO tmp_printer_distributor_map (serial_fiscal, distributor_rif)
    VALUES
        ('GRA0000012', 'J-502995781'),
        ('GRA0000013', 'J-506614120'),
        ('GRA0000014', 'J-504594369'),
        ('GRA0000015', 'J-504594369'),
        ('GRA0000020', 'J-506425297'),
        ('GRA0000021', 'J-504594369'),
        ('GRA0000022', 'J-506425297'),
        ('GRA0000023', 'J-506425297'),
        ('GRA0000024', 'J-506610957'),
        ('GRA0000025', 'J-502995781'),
        ('GRA0000026', 'J-506425297'),
        ('GRA0000027', 'J-504594369'),
        ('GRA0000028', 'J-506425297'),
        ('GRA0000029', 'J-504594369'),
        ('GRA0000030', 'J-506610957'),
        ('GRA0000031', 'J-506610957'),
        ('GRA0000032', 'J-506610957'),
        ('GRA0000033', 'J-506610957'),
        ('GRA0000034', 'J-506610957'),
        ('GRA0000035', 'J-506610957'),
        ('GRA0000036', 'J-506610957'),
        ('GRA0000037', 'J-504594369'),
        ('GRA0000038', 'J-504594369'),
        ('GRA0000039', 'J-506425297'),
        ('GRA0000040', 'J-506425297'),
        ('GRA0000041', 'J-502995781'),
        ('GRA0000042', 'J-506425297'),
        ('GRA0000043', 'J-502995781'),
        ('GRA0000044', 'J-506610957'),
        ('GRA0000045', 'J-506610957'),
        ('GRA0000046', 'J-502995781'),
        ('GRA0000047', 'J-504594369'),
        ('GRA0000048', 'J-504594369'),
        ('GRA0000049', 'J-502995781'),
        ('GRA0000050', 'J-506614120'),
        ('GRA0000051', 'J-502338160'),
        ('GRA0000052', 'J-504594369'),
        ('GRA0000053', 'J-503981830'),
        ('GRA0000054', 'J-506614120'),
        ('GRA0000055', 'J-503981830'),
        ('GRA0000056', 'J-506425297'),
        ('GRA0000057', 'J-504594369'),
        ('GRA0000058', 'J-504594369'),
        ('GRA0000059', 'J-503981830'),
        ('GRA0000060', 'J-506743701'),
        ('GRA0000061', 'J-503981830'),
        ('GRA0000062', 'J-504594369'),
        ('GRA0000063', 'J-503981830'),
        ('GRA0000064', 'J-502995781'),
        ('GRA0000065', 'J-504594369'),
        ('GRA0000066', 'J-502995781'),
        ('GRA0000067', 'J-502995781'),
        ('GRA0000068', 'J-502995781'),
        ('GRA0000069', 'J-502995781'),
        ('GRA0000070', 'J-502995781'),
        ('GRA0000071', 'J-506610957'),
        ('GRA0000072', 'J-504594369'),
        ('GRA0000073', 'J-502995781'),
        ('GRA0000074', 'J-502995781'),
        ('GRA0000075', 'J-506610957'),
        ('GRA0000076', 'J-506610957'),
        ('GRA0000078', 'J-506610957'),
        ('GRA0000079', 'J-506610957'),
        ('GRA0000080', 'J-506610957'),
        ('GRA0000081', 'J-506610957'),
        ('GRA0000082', 'J-506610957'),
        ('GRA0000083', 'J-506425297'),
        ('GRA0000084', 'J-506610957'),
        ('GRA0000085', 'J-506610957'),
        ('GRA0000086', 'J-504594369'),
        ('GRA0000087', 'J-504594369'),
        ('GRA0000088', 'J-506425297'),
        ('GRA0000089', 'J-294813488'),
        ('GRA0000090', 'J-294813488'),
        ('GRA0000091', 'J-294813488'),
        ('GRA0000092', 'J-506425297'),
        ('GRA0000093', 'J-294813488'),
        ('GRA0000094', 'J-294813488'),
        ('GRA0000095', 'J-504594369'),
        ('GRA0000096', 'J-502995781'),
        ('GRA0000097', 'J-506425297'),
        ('GRA0000098', 'J-506425297'),
        ('GRA0000099', 'J-506614120'),
        ('GRA0000100', 'J-506614120'),
        ('GRA0000101', 'J-506614120'),
        ('GRA0000102', 'J-506614120'),
        ('GRA0000103', 'J-506614120'),
        ('GRA0000104', 'J-506614120'),
        ('GRA0000105', 'J-506614120'),
        ('GRA0000106', 'J-506614120'),
        ('GRA0000107', 'J-504594369'),
        ('GRA0000108', 'J-503981830'),
        ('GRA0000109', 'J-294813488'),
        ('GRA0000110', 'J-294813488'),
        ('GRA0000111', 'J-294813488'),
        ('GRA0000112', 'J-294813488'),
        ('GRA0000113', 'J-294813488'),
        ('GRA0000114', 'J-504594369'),
        ('GRA0000115', 'J-503981830'),
        ('GRA0000116', 'J-506425297'),
        ('GRA0000117', 'J-504594369'),
        ('GRA0000118', 'J-506425297'),
        ('GRA0000119', 'J-506888424'),
        ('GRA0000120', 'J-504594369'),
        ('GRA0000121', 'J-504594369'),
        ('GRA0000122', 'J-506425297'),
        ('GRA0000123', 'J-504594369'),
        ('GRA0000124', 'J-502995781'),
        ('GRA0000125', 'J-502995781'),
        ('GRA0000126', 'J-502995781'),
        ('GRA0000127', 'J-502995781'),
        ('GRA0000128', 'J-502995781'),
        ('GRA0000129', 'J-502995781'),
        ('GRA0000130', 'J-502995781'),
        ('GRA0000131', 'J-502995781'),
        ('GRA0000132', 'J-506425297'),
        ('GRA0000134', 'J-502995781'),
        ('GRA0000135', 'J-502995781'),
        ('GRA0000136', 'J-502995781'),
        ('GRA0000137', 'J-502995781'),
        ('GRA0000138', 'J-502995781'),
        ('GRA0000139', 'J-502995781'),
        ('GRA0000140', 'J-502995781'),
        ('GRA0000141', 'J-502995781'),
        ('GRA0000142', 'J-504594369'),
        ('GRA0000143', 'J-504594369'),
        ('GRA0000144', 'J-502995781'),
        ('GRA0000145', 'J-502995781'),
        ('GRA0000146', 'J-502995781'),
        ('GRA0000147', 'J-502995781'),
        ('GRA0000148', 'J-123456789'),
        ('GRA0000149', 'J-503981830'),
        ('GRA0000150', 'J-503981830'),
        ('GRA0000152', 'J-503981830'),
        ('GRA0000153', 'J-503981830'),
        ('GRA0000154', 'J-506614120'),
        ('GRA0000155', 'J-506425297'),
        ('GRA0000156', 'J-502995781'),
        ('GRA0000157', 'J-502995781'),
        ('GRA0000158', 'J-502995781'),
        ('GRA0000159', 'J-502995781'),
        ('GRA0000160', 'J-502995781'),
        ('GRA0000161', 'J-502995781'),
        ('GRA0000162', 'J-502995781'),
        ('GRA0000163', 'J-504594369'),
        ('GRA0000164', 'J-506888424'),
        ('GRA0000165', 'J-506614120'),
        ('GRA0000166', 'J-506614120'),
        ('GRA0000167', 'J-506614120'),
        ('GRA0000168', 'J-506610957'),
        ('GRA0000169', 'J-506614120'),
        ('GRA0000170', 'J-506614120'),
        ('GRA0000171', 'J-506614120'),
        ('GRA0000172', 'J-506614120'),
        ('GRA0000173', 'J-506614120'),
        ('GRA0000174', 'J-502995781'),
        ('GRA0000175', 'J-506425297'),
        ('GRA0000176', 'J-503981830'),
        ('GRA0000177', 'J-503981830'),
        ('GRA0000178', 'J-504594369'),
        ('GRA0000180', 'J-506888424'),
        ('GRA0000181', 'J-506888424'),
        ('GRA0000182', 'J-504594369'),
        ('GRA0000183', 'J-502564772'),
        ('GRA0000184', 'J-506610957'),
        ('GRA0000185', 'J-504594369'),
        ('GRA0000186', 'J-506610957'),
        ('GRA0000187', 'J-506610957'),
        ('GRA0000188', 'J-502995781'),
        ('GRA0000189', 'J-506610957'),
        ('GRA0000190', 'J-502995781'),
        ('GRA0000191', 'J-502995781'),
        ('GRA0000192', 'J-502995781'),
        ('GRA0000193', 'J-502995781'),
        ('GRA0000194', 'J-504594369'),
        ('GRA0000195', 'J-504594369'),
        ('GRA0000196', 'J-506610957'),
        ('GRA0000197', 'J-506610957'),
        ('GRA0000199', 'J-502995781'),
        ('GRA0000200', 'J-502995781'),
        ('GRA0000201', 'J-502995781'),
        ('GRA0000202', 'J-502995781'),
        ('GRA0000203', 'J-502995781'),
        ('GRA0000204', 'J-502995781'),
        ('GRA0000205', 'J-502995781'),
        ('GRA0000206', 'J-504594369'),
        ('GRA0000207', 'J-314520148'),
        ('GRA0000208', 'J-503981830'),
        ('GRA0000209', 'J-503981830'),
        ('GRA0000210', 'J-503981830'),
        ('GRA0000211', 'J-503981830'),
        ('GRA0000212', 'J-503981830'),
        ('GRA0000213', 'J-503981830'),
        ('GRA0000214', 'J-503981830'),
        ('GRA0000215', 'J-503981830'),
        ('GRA0000216', 'J-504594369'),
        ('GRA0000217', 'J-504594369'),
        ('GRA0000218', 'J-504594369'),
        ('GRA0000219', 'J-123456789'),
        ('GRA0000220', 'J-504594369'),
        ('GRA0000221', 'J-123456789'),
        ('GRA0000222', 'J-123456789'),
        ('GRA0000223', 'J-123456789'),
        ('GRA0000224', 'J-123456789'),
        ('GRA0000225', 'J-123456789'),
        ('GRA0000226', 'J-123456789'),
        ('GRA0000227', 'J-123456789'),
        ('GRA0000228', 'J-123456789'),
        ('GRA0000229', 'J-123456789'),
        ('GRA0000230', 'J-123456789'),
        ('GRA0000231', 'J-123456789'),
        ('GRA0000232', 'J-123456789'),
        ('GRA0000233', 'J-123456789'),
        ('GRA0000234', 'J-123456789'),
        ('GRA0000235', 'J-123456789'),
        ('GRA0000236', 'J-123456789'),
        ('GRA0000237', 'J-123456789'),
        ('GRA0000238', 'J-123456789'),
        ('GRA0000239', 'J-504594369'),
        ('GRA0000240', 'J-504594369'),
        ('GRA0000241', 'J-506614120'),
        ('GRA0000242', 'J-506614120'),
        ('GRA0000243', 'J-504594369'),
        ('GRA0000244', 'J-506614120'),
        ('GRA0000245', 'J-504594369'),
        ('GRA0000246', 'J-506614120'),
        ('GRA0000247', 'J-123456789'),
        ('GRA0000248', 'J-123456789'),
        ('GRA0000249', 'J-123456789'),
        ('GRA0000250', 'J-123456789'),
        ('GRA0000251', 'J-123456789'),
        ('GRA0000252', 'J-123456789'),
        ('GRA0000253', 'J-123456789'),
        ('GRA0000254', 'J-123456789'),
        ('GRA0000255', 'J-123456789'),
        ('GRA0000256', 'J-123456789'),
        ('GRA0000257', 'J-123456789'),
        ('GRA0000258', 'J-123456789'),
        ('GRA0000259', 'J-123456789'),
        ('GRA0000260', 'J-506425297'),
        ('GRA0000261', 'J-123456789'),
        ('GRA0000262', 'J-123456789'),
        ('GRA0000263', 'J-123456789'),
        ('GRA0000264', 'J-123456789'),
        ('GRA0000265', 'J-123456789'),
        ('GRA0000267', 'J-123456789'),
        ('GRA0000268', 'J-504594369'),
        ('GRA0000269', 'J-123456789'),
        ('GRA0000270', 'J-123456789'),
        ('GRA0000271', 'J-123456789'),
        ('GRA0000272', 'J-123456789'),
        ('GRA0000273', 'J-123456789'),
        ('GRA0000274', 'J-123456789'),
        ('GRA0000275', 'J-123456789'),
        ('GRA0000276', 'J-123456789'),
        ('GRA0000277', 'J-123456789'),
        ('GRA0000278', 'J-123456789'),
        ('GRA0000279', 'J-123456789'),
        ('GRA0000280', 'J-504594369'),
        ('GRA0000281', 'J-502338160'),
        ('GRA0000282', 'J-502338160'),
        ('GRA0000283', 'J-502338160'),
        ('GRA0000284', 'J-504594369'),
        ('GRA0000285', 'J-503981830'),
        ('GRA0000286', 'J-504594369'),
        ('GRA0000287', 'J-504594369'),
        ('GRA0000288', 'J-506425297'),
        ('GRA0000289', 'J-500956738'),
        ('GRA0000290', 'J-506425297'),
        ('GRA0000291', 'J-506425297'),
        ('GRA0000292', 'J-500956738'),
        ('GRA0000293', 'J-500956738'),
        ('GRA0000294', 'J-504594369'),
        ('GRA0000295', 'J-506425297'),
        ('GRA0000296', 'J-506425297'),
        ('GRA0000297', 'J-506425297'),
        ('GRA0000298', 'J-504594369'),
        ('GRA0000299', 'J-506425297'),
        ('GRA0000300', 'J-506425297'),
        ('GRA0000301', 'J-504594369'),
        ('GRA0000302', 'J-504594360'),
        ('GRA0000310', 'J-502995781'),
        ('GRA0000311', 'J-502995781'),
        ('GRA0000312', 'J-504594369'),
        ('GRA0000313', 'J-502995781'),
        ('GRA0000314', 'J-504594369'),
        ('GRA0000315', 'J-504594369'),
        ('GRA0000316', 'J-502995781'),
        ('GRA0000317', 'J-502995781'),
        ('GRA0000318', 'J-502995781'),
        ('GRA0000319', 'J-502995781'),
        ('GRA0000320', 'J-502995781'),
        ('GRA0000321', 'J-502995781'),
        ('GRA0000322', 'J-502995781'),
        ('GRA0000323', 'J-502995781'),
        ('GRA0000324', 'J-502995781'),
        ('GRA0000325', 'J-502995781'),
        ('GRA0000326', 'J-502995781'),
        ('GRA0000327', 'J-502995781'),
        ('GRA0000328', 'J-502995781'),
        ('GRA0000329', 'J-502995781'),
        ('GRA0000330', 'J-502995781'),
        ('GRA0000331', 'J-502995781'),
        ('GRA0000332', 'J-506425297'),
        ('GRA0000333', 'J-313990060'),
        ('GRA0000334', 'J-503981830'),
        ('GRA0000335', 'J-503981830'),
        ('GRA0000336', 'J-506425297'),
        ('GRA0000337', 'J-506425297'),
        ('GRA0000338', 'J-506425297'),
        ('GRA0000339', 'J-506425297'),
        ('GRA0000340', 'J-504594369'),
        ('GRA0000341', 'J-504594369'),
        ('GRA0000342', 'J-506425297'),
        ('GRA0000344', 'J-503981830'),
        ('GRA0000345', 'J-504594369'),
        ('GRA0000346', 'J-506425297'),
        ('GRA0000347', 'J-506425297'),
        ('GRA0000348', 'J-506425297'),
        ('GRA0000349', 'J-506425297'),
        ('GRA0000350', 'J-504594369'),
        ('GRA0000351', 'J-504594360'),
        ('GRA0000352', 'J-503981830'),
        ('GRA0000353', 'J-503981830'),
        ('GRA0000354', 'J-506610957'),
        ('GRA0000355', 'J-506425297'),
        ('GRA0000356', 'J-506425297'),
        ('GRA0000357', 'J-506425297'),
        ('GRA0000358', 'J-506610957'),
        ('GRA0000359', 'J-502995781'),
        ('GRA0000361', 'J-502995781'),
        ('GRA0000362', 'J-297203729'),
        ('GRA0000363', 'J-500956738'),
        ('GRA0000364', 'J-502995781'),
        ('GRA0000365', 'J-502995781'),
        ('GRA0000366', 'J-502995781'),
        ('GRA0000367', 'J-502995781'),
        ('GRA0000368', 'J-502995781'),
        ('GRA0000369', 'J-502995781'),
        ('GRA0000370', 'J-502995781'),
        ('GRA0000371', 'J-502995781'),
        ('GRA0000372', 'J-502995781'),
        ('GRA0000373', 'J-502995781'),
        ('GRA0000374', 'J-502995781'),
        ('GRA0000375', 'J-502995781'),
        ('GRA0000376', 'J-502995781'),
        ('GRA0000377', 'J-502995781'),
        ('GRA0000378', 'J-502995781'),
        ('GRA0000379', 'J-502995781'),
        ('GRA0000380', 'J-504594360'),
        ('GRA0000381', 'J-504594360'),
        ('GRA0000382', 'J-506425297'),
        ('GRA0000383', 'J-506425297'),
        ('GRA0000384', 'J-506425297'),
        ('GRA0000385', 'J-506425297'),
        ('GRA0000386', 'J-506425297'),
        ('GRA0000387', 'J-506425297'),
        ('GRA0000388', 'J-506425297'),
        ('GRA0000389', 'J-506425297'),
        ('GRA0000390', 'J-502995781'),
        ('GRA0000391', 'J-504594360'),
        ('GRA0000392', 'J-502995781'),
        ('GRA0000393', 'J-502995781'),
        ('GRA0000394', 'J-502995781'),
        ('GRA0000395', 'J-502995781'),
        ('GRA0000396', 'J-505582623'),
        ('GRA0000397', 'J-502995781'),
        ('GRA0000398', 'J-502995781'),
        ('GRA0000399', 'J-504594369'),
        ('GRA0000400', 'J-504594369'),
        ('GRA0000401', 'J-504594360'),
        ('GRA0000402', 'J-504594360'),
        ('GRA0000403', 'J-502995781'),
        ('GRA0000404', 'J-504594369'),
        ('GRA0000405', 'J-506425297'),
        ('GRA0000406', 'J-506425297'),
        ('GRA0000407', 'J-506425297'),
        ('GRA0000408', 'J-506425297'),
        ('GRA0000409', 'J-506425297'),
        ('GRA0000410', 'J-502995781'),
        ('GRA0000411', 'J-506425297'),
        ('GRA0000412', 'J-506888424'),
        ('GRA0000413', 'J-506888424'),
        ('GRA0000414', 'J-506888424'),
        ('GRA0000415', 'J-506888424'),
        ('GRA0000416', 'J-506888424'),
        ('GRA0000417', 'J-506888424'),
        ('GRA0000418', 'J-506888424'),
        ('GRA0000419', 'J-506888424'),
        ('GRA0000420', 'J-314520148'),
        ('GRA0000422', 'J-504594369'),
        ('GRA0000423', 'J-506948753'),
        ('GRA0000424', 'J-506948753'),
        ('GRA0000425', 'J-506948753'),
        ('GRA0000426', 'J-506948753'),
        ('GRA0000427', 'J-506948753'),
        ('GRA0000428', 'J-506948753'),
        ('GRA0000429', 'J-504594369'),
        ('GRA0000430', 'J-506948753'),
        ('GRA0000431', 'J-501197539'),
        ('GRA0000432', 'J-501197539'),
        ('GRA0000433', 'J-504594369'),
        ('GRA0000434', 'J-501197539'),
        ('GRA0000435', 'J-501197539'),
        ('GRA0000436', 'J-501197539'),
        ('GRA0000437', 'J-501197539'),
        ('GRA0000438', 'J-501197539'),
        ('GRA0000439', 'J-506425297'),
        ('GRA0000440', 'J-505582623'),
        ('GRA0000441', 'J-506743702'),
        ('GRA0000442', 'J-506743702'),
        ('GRA0000444', 'J-506743701'),
        ('GRA0000445', 'J-506743701'),
        ('GRA0000447', 'J-313990060'),
        ('GRA0000448', 'J-313990060'),
        ('GRA0000449', 'J-313990060'),
        ('GRA0000450', 'J-313990060'),
        ('GRA0000451', 'J-313990060'),
        ('GRA0000452', 'J-313990060'),
        ('GRA0000453', 'J-313990060'),
        ('GRA0000454', 'J-508057503'),
        ('GRA0000455', 'J-504594369'),
        ('GRA0000456', 'J-508324781'),
        ('GRA0000457', 'J-508324781'),
        ('GRA0000458', 'J-506743702'),
        ('GRA0000460', 'J-502995781'),
        ('GRA0000461', 'J-502995781'),
        ('GRA0000462', 'J-502995781'),
        ('GRA0000463', 'J-502995781'),
        ('GRA0000464', 'J-504594369'),
        ('GRA0000465', 'J-502995781'),
        ('GRA0000466', 'J-500956738'),
        ('GRA0000467', 'J-502995781'),
        ('GRA0000468', 'J-502995781'),
        ('GRA0000469', 'J-502995781'),
        ('GRA0000470', 'J-502995781'),
        ('GRA0000471', 'J-502995781'),
        ('GRA0000472', 'J-502995781'),
        ('GRA0000473', 'J-502995781'),
        ('GRA0000474', 'J-502995781'),
        ('GRA0000475', 'J-502995781'),
        ('GRA0000476', 'J-507231330'),
        ('GRA0000477', 'J-507231330'),
        ('GRA0000478', 'J-506743702'),
        ('GRA0000479', 'J-502564772'),
        ('GRA0000480', 'J-502564772'),
        ('GRA0000481', 'J-409140105'),
        ('GRA0000482', 'J-409140105'),
        ('GRA0000483', 'J-505582623'),
        ('GRA0000484', 'J-297203729'),
        ('GRA0000485', 'J-297203729'),
        ('GRA0000486', 'J-506610957'),
        ('GRA0000487', 'J-504594369'),
        ('GRA0000488', 'J-503981830'),
        ('GRA0000489', 'J-506610957'),
        ('GRA0000490', 'J-504594369'),
        ('GRA0000491', 'J-504594369'),
        ('GRA0000492', 'J-502995781'),
        ('GRA0000493', 'J-506610957'),
        ('GRA0000494', 'J-506610957'),
        ('GRA0000495', 'J-506610957'),
        ('GRA0000496', 'J-502995781'),
        ('GRA0000497', 'J-504594369'),
        ('GRA0000498', 'J-500956738'),
        ('GRA0000499', 'J-500956738'),
        ('GRA0000500', 'J-506425297'),
        ('GRA0000501', 'J-506425297'),
        ('GRA0000502', 'J-506425297'),
        ('GRA0000503', 'J-506425297'),
        ('GRA0000504', 'J-506425297'),
        ('GRA0000505', 'J-506425297'),
        ('GRA0000506', 'J-506425297'),
        ('GRA0000507', 'J-506425297'),
        ('GRA0000508', 'J-502995781'),
        ('GRA0000509', 'J-502995781'),
        ('GRA0000510', 'J-502995781'),
        ('GRA0000511', 'J-502995781'),
        ('GRA0000512', 'J-502995781'),
        ('GRA0000513', 'J-502995781'),
        ('GRA0000514', 'J-502995781'),
        ('GRA0000515', 'J-502995781'),
        ('GRA0000516', 'J-297203729'),
        ('GRA0000517', 'J-297203729'),
        ('GRA0000518', 'J-294926398'),
        ('GRA0000519', 'J-294926398'),
        ('GRA0000520', 'J-294926398'),
        ('GRA0000521', 'J-506610957'),
        ('GRA0000522', 'J-506610957'),
        ('GRA0000523', 'J-506610957'),
        ('GRA0000524', 'J-506743702'),
        ('GRA0000525', 'J-504594369'),
        ('GRA0000526', 'J-503981830'),
        ('GRA0000527', 'J-503981830'),
        ('GRA0000528', 'J-297203729'),
        ('GRA0000529', 'J-504594369'),
        ('GRA0000530', 'J-503981830'),
        ('GRA0000531', 'J-504594369'),
        ('GRA0000532', 'J-504594369'),
        ('GRA0000533', 'J-500956738'),
        ('GRA0000534', 'J-500956738'),
        ('GRA0000535', 'J-504594369'),
        ('GRA0000536', 'J-504594369'),
        ('GRA0000537', 'J-503981830'),
        ('GRA0000538', 'J-504594369'),
        ('GRA0000539', 'J-504594369'),
        ('GRA0000540', 'J-506425297'),
        ('GRA0000541', 'J-506425297'),
        ('GRA0000542', 'J-506425297'),
        ('GRA0000543', 'J-506425297'),
        ('GRA0000544', 'J-506425297'),
        ('GRA0000545', 'J-506425297'),
        ('GRA0000546', 'J-506425297'),
        ('GRA0000547', 'J-506425297'),
        ('GRA0000548', 'J-506888424'),
        ('GRA0000549', 'J-506888424'),
        ('GRA0000550', 'J-506888424'),
        ('GRA0000551', 'J-506888424'),
        ('GRA0000552', 'J-506888424'),
        ('GRA0000553', 'J-506888424'),
        ('GRA0000554', 'J-506888424'),
        ('GRA0000555', 'J-506888424'),
        ('GRA0000556', 'J-506888424'),
        ('GRA0000557', 'J-506888424'),
        ('GRA0000558', 'J-506888424'),
        ('GRA0000559', 'J-506888424'),
        ('GRA0000560', 'J-506888424'),
        ('GRA0000561', 'J-506888424'),
        ('GRA0000562', 'J-506888424'),
        ('GRA0000563', 'J-506888424'),
        ('GRA0000564', 'J-502564772'),
        ('GRA0000565', 'J-502564772'),
        ('GRA0000569', 'J-508057503'),
        ('GRA0000572', 'J-506948753'),
        ('GRA0000574', 'J-506948753'),
        ('GRA0000575', 'J-506948753'),
        ('GRA0000576', 'J-506948753'),
        ('GRA0000577', 'J-506948753'),
        ('GRA0000578', 'J-506948753'),
        ('GRA0000579', 'J-506948753'),
        ('GRA0000596', 'J-501603260'),
        ('GRA0000597', 'J-501603260'),
        ('GRA0000598', 'J-501603260'),
        ('GRA0000599', 'J-501603260'),
        ('GRA0000600', 'J-501603260'),
        ('GRA0000602', 'J-501603260'),
        ('GRA0000603', 'J-501603260'),
        ('GRA0000606', 'J-508057503'),
        ('GRA0000612', 'J-313990060'),
        ('GRA0000613', 'J-313990060'),
        ('GRA0000614', 'J-313990060'),
        ('GRA0000615', 'J-313990060'),
        ('GRA0000616', 'J-313990060'),
        ('GRA0000617', 'J-313990060'),
        ('GRA0000618', 'J-313990060'),
        ('GRA0000619', 'J-313990060'),
        ('GRA0000620', 'J-313990060'),
        ('GRA0000621', 'J-313990060'),
        ('GRA0000622', 'J-313990060'),
        ('GRA0000623', 'J-313990060'),
        ('GRA0000624', 'J-313990060'),
        ('GRA0000625', 'J-313990060'),
        ('GRA0000626', 'J-313990060'),
        ('GRA0000627', 'J-313990060'),
        ('GRA0000628', 'J-501197539'),
        ('GRA0000629', 'J-501197539'),
        ('GRA0000630', 'J-501197539'),
        ('GRA0000631', 'J-501197539'),
        ('GRA0000632', 'J-501197539'),
        ('GRA0000633', 'J-501197539'),
        ('GRA0000634', 'J-501197539'),
        ('GRA0000635', 'J-501197539'),
        ('GRA0000636', 'J-501197539'),
        ('GRA0000637', 'J-501197539'),
        ('GRA0000638', 'J-501197539'),
        ('GRA0000639', 'J-501197539'),
        ('GRA0000640', 'J-501197539'),
        ('GRA0000641', 'J-501197539'),
        ('GRA0000642', 'J-501197539'),
        ('GRA0000643', 'J-501197539'),
        ('GRA0000644', 'J-501197539'),
        ('GRA0000645', 'J-501197539'),
        ('GRA0000646', 'J-501197539'),
        ('GRA0000647', 'J-501197539'),
        ('GRA0000649', 'J-501197539'),
        ('GRA0000651', 'J-505582623'),
        ('GRA0000652', 'J-504594360'),
        ('GRA0000653', 'J-297203729'),
        ('GRA0000654', 'J-297203729'),
        ('GRA0000655', 'J-504594369'),
        ('GRA0000656', 'J-504594369'),
        ('GRA0000657', 'J-504594369'),
        ('GRA0000658', 'J-504594369'),
        ('GRA0000659', 'J-506610957'),
        ('GRA0000660', 'J-409140105'),
        ('GRA0000661', 'J-506948753'),
        ('GRA0000662', 'J-506948753'),
        ('GRA0000663', 'J-506948753'),
        ('GRA0000664', 'J-506948753'),
        ('GRA0000666', 'J-508324781'),
        ('GRA0000676', 'J-294926398'),
        ('GRA0000677', 'J-294926398'),
        ('GRA0000678', 'J-294926398'),
        ('GRA0000679', 'J-501578680'),
        ('GRA0000680', 'J-501578680'),
        ('GRA0000681', 'J-502995781'),
        ('GRA0000682', 'J-506425297'),
        ('GRA0000683', 'J-504594360'),
        ('GRA0000684', 'J-502995781'),
        ('GRA0000685', 'J-502995781'),
        ('GRA0000686', 'J-502995781'),
        ('GRA0000687', 'J-502995781'),
        ('GRA0000688', 'J-502995781'),
        ('GRA0000689', 'J-502995781'),
        ('GRA0000690', 'J-502995781'),
        ('GRA0000691', 'J-502995781'),
        ('GRA0000692', 'J-502995781'),
        ('GRA0000693', 'J-502995781'),
        ('GRA0000694', 'J-502995781'),
        ('GRA0000695', 'J-502995781'),
        ('GRA0000696', 'J-502995781'),
        ('GRA0000697', 'J-502995781'),
        ('GRA0000698', 'J-502995781'),
        ('GRA0000699', 'J-506425297'),
        ('GRA0000700', 'J-412152254'),
        ('GRA0000701', 'J-506743702'),
        ('GRA0000708', 'J-504594369'),
        ('GRA0000709', 'J-297203729'),
        ('GRA0000714', 'J-504594369');

    -- Verify every target RIF resolves to a valid public.distribuidoras row
    SELECT COUNT(*)
    INTO v_unmatched_rifs
    FROM (
        SELECT DISTINCT m.distributor_rif
        FROM tmp_printer_distributor_map m
        LEFT JOIN (
            SELECT d.id AS id_distribuidora,
                   regexp_replace(upper(e.rif), '[^A-Z0-9]', '', 'g') AS clean_rif
            FROM public.distribuidoras d
            JOIN public.sucursales s ON s.id = d.id_sucursal
            JOIN public.empresas e ON e.id = s.id_empresa
        ) dist_rif
          ON dist_rif.clean_rif = regexp_replace(upper(m.distributor_rif), '[^A-Z0-9]', '', 'g')
        WHERE dist_rif.id_distribuidora IS NULL
    ) missing;

    IF v_unmatched_rifs > 0 THEN
        RAISE EXCEPTION 'V48 aborted: % distributor RIF(s) could not be resolved in public.distribuidoras', v_unmatched_rifs;
    END IF;

    -- 3A. Update public.impresoras.id_distribuidora
    WITH resolved_map AS (
        SELECT m.serial_fiscal,
               MIN(d.id) AS target_distributor_id
        FROM tmp_printer_distributor_map m
        JOIN public.empresas e
          ON regexp_replace(upper(e.rif), '[^A-Z0-9]', '', 'g') = regexp_replace(upper(m.distributor_rif), '[^A-Z0-9]', '', 'g')
        JOIN public.sucursales s ON s.id_empresa = e.id
        JOIN public.distribuidoras d ON d.id_sucursal = s.id
        GROUP BY m.serial_fiscal
    )
    UPDATE public.impresoras i
    SET id_distribuidora = rm.target_distributor_id
    FROM resolved_map rm
    WHERE upper(trim(i.serial_fiscal)) = rm.serial_fiscal
      AND i.id_distribuidora IS DISTINCT FROM rm.target_distributor_id;

    GET DIAGNOSTICS v_printers_dist_updated = ROW_COUNT;

    -- 3B. Transition 'sin_asignar' -> 'asignada' for printers that now have a distributor
    UPDATE public.impresoras i
    SET estatus = 'asignada'
    FROM tmp_printer_distributor_map m
    WHERE upper(trim(i.serial_fiscal)) = m.serial_fiscal
      AND i.id_distribuidora IS NOT NULL
      AND lower(trim(i.estatus)) = 'sin_asignar';

    GET DIAGNOSTICS v_printers_status_updated = ROW_COUNT;

    -- 3C. Synchronize public.clientes.id_distribuidora from their assigned printers
    -- Uses MIN(i.id_distribuidora) for deterministic resolution and respects V36 (no self-client)
    WITH client_target_dist AS (
        SELECT i.id_cliente,
               MIN(i.id_distribuidora) AS target_distributor_id
        FROM public.impresoras i
        JOIN tmp_printer_distributor_map m
          ON upper(trim(i.serial_fiscal)) = m.serial_fiscal
        WHERE i.id_cliente IS NOT NULL
          AND i.id_distribuidora IS NOT NULL
        GROUP BY i.id_cliente
    )
    UPDATE public.clientes c
    SET id_distribuidora = ctd.target_distributor_id
    FROM client_target_dist ctd
    JOIN public.distribuidoras d ON d.id = ctd.target_distributor_id
    JOIN public.sucursales sd ON sd.id = d.id_sucursal,
         public.sucursales sc
    WHERE c.id = ctd.id_cliente
      AND sc.id = c.id_sucursal
      AND c.id_sucursal <> d.id_sucursal
      AND sc.id_empresa <> sd.id_empresa
      AND c.id_distribuidora IS DISTINCT FROM ctd.target_distributor_id;

    GET DIAGNOSTICS v_clients_dist_updated = ROW_COUNT;

    RAISE NOTICE 'V48 complete: printers distributor updated=%, printers status sin_asignar->asignada=%, clients distributor updated=%',
        v_printers_dist_updated, v_printers_status_updated, v_clients_dist_updated;
END $$;
