-- 3FN: Facturas comerciales, pagos comerciales y vínculo 1:N desde impresoras

CREATE TABLE IF NOT EXISTS facturas_comerciales (
    id BIGSERIAL PRIMARY KEY,
    numero_factura VARCHAR(30),
    sufijo_control VARCHAR(10),
    factura_cliente VARCHAR(50),
    modalidad VARCHAR(30) NOT NULL DEFAULT 'VENTA',
    estado_cobro VARCHAR(30) NOT NULL DEFAULT 'PAGADO',
    promotor_vendedor VARCHAR(100),
    notas_comerciales TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_facturas_comerciales_modalidad CHECK (
        modalidad IN ('VENTA', 'CONSIGNACION', 'DONACION_REGALO', 'PRESTAMO', 'MUESTRA_SENIAT')
    ),
    CONSTRAINT chk_facturas_comerciales_estado_cobro CHECK (
        estado_cobro IN ('PAGADO', 'PAGO_PARCIAL', 'DEBE_IVA', 'PENDIENTE_PAGO', 'POR_FACTURAR', 'SIN_CARGO')
    )
);

CREATE TABLE IF NOT EXISTS pagos_comerciales (
    id BIGSERIAL PRIMARY KEY,
    id_factura_comercial BIGINT NOT NULL REFERENCES facturas_comerciales(id) ON DELETE CASCADE,
    metodo_banco VARCHAR(30) NOT NULL,
    referencia VARCHAR(100),
    monto NUMERIC(12, 2),
    cuenta_destino VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pagos_comerciales_metodo_banco CHECK (
        metodo_banco IN ('BNC', 'BINANCE', 'ZELLE', 'DOLARES_EFECTIVO', 'BCV', 'OTRO')
    )
);

CREATE INDEX IF NOT EXISTS idx_pagos_comerciales_id_factura
    ON pagos_comerciales(id_factura_comercial);

ALTER TABLE impresoras
    ADD COLUMN IF NOT EXISTS id_factura_comercial BIGINT REFERENCES facturas_comerciales(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS pendiente_etiqueta BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS condicion_hardware VARCHAR(30) NOT NULL DEFAULT 'OPERATIVA';

ALTER TABLE impresoras
    DROP CONSTRAINT IF EXISTS chk_impresoras_condicion_hardware;

ALTER TABLE impresoras
    ADD CONSTRAINT chk_impresoras_condicion_hardware CHECK (
        condicion_hardware IN ('OPERATIVA', 'SIN_TARJETA', 'DESARMADA', 'DANADA', 'INTEGRACION', 'PRUEBA_MQTT')
    );

CREATE INDEX IF NOT EXISTS idx_impresoras_id_factura_comercial
    ON impresoras(id_factura_comercial);

-- Seriales marcados con pendiente_etiqueta = TRUE
UPDATE impresoras
SET pendiente_etiqueta = TRUE
WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000020', '0000022', '0000023', '0000026');

DO $$
DECLARE
    v_id BIGINT;
    v_factura_67 BIGINT;
BEGIN
    -- =========================================================================
    -- 1. FACTURAS PAGADAS POR BNC
    -- =========================================================================

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000001', 'VENTA', 'PAGADO', 'ALBERT') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000027';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000002', 'VENTA', 'PAGADO', 'Toca saber lo que deben') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000023';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000005', 'VENTA', 'PAGADO', 'Cliente: Chino San Félix / Edgar') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000021', '0000037', '0000038');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000009', 'VENTA', 'PAGADO', 'Comarcas') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000024', '0000030', '0000031', '0000032', '0000033',
        '0000034', '0000035', '0000036', '0000044', '0000045'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor, notas_comerciales)
    VALUES ('0000011', 'VENTA', 'PAGADO', 'ALBERT', 'Yaracuy') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000065';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000012', 'VENTA', 'PAGADO', 'Kodex') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000053', '0000055', '0000059', '0000061', '0000063'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000013', 'VENTA', 'PAGADO', 'Chino en el Core 8') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000062';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000014', 'VENTA', 'PAGADO', 'ALBERT') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000077';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000015', 'VENTA', 'PAGADO', 'Cliente: Traki') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000086', '0000087');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000019', 'VENTA', 'PAGADO', 'ALBERT') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco, cuenta_destino)
    VALUES (v_id, 'BNC', 'CUENTA ALPHA');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000057';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000021', 'VENTA', 'PAGADO', 'Cliente: Costa America') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000047', '0000048', '0000058');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000022', 'VENTA', 'PAGADO', 'D3XD') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000089', '0000090', '0000093', '0000094');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000023', 'VENTA', 'PAGADO', 'ALBERT') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco, referencia)
    VALUES (v_id, 'BNC', '13156623387');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000095';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000024', 'VENTA', 'PAGADO', 'D3XD') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000091', '0000109', '0000110', '0000111', '0000112', '0000113'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000026', 'VENTA', 'PAGADO', 'PROMOTOR D3XD') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000121';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000028', 'VENTA', 'PAGADO', 'STC') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000020', '0000022', '0000028', '0000039', '0000040',
        '0000042', '0000083', '0000092', '0000097'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000035', 'VENTA', 'PAGADO', 'Cliente: SKY') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000123';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000041', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000164';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000075', 'VENTA', 'PAGADO', 'Kodex') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000334', '0000335');

    INSERT INTO facturas_comerciales (numero_factura, sufijo_control, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000079', 'X', 'VENTA', 'PAGADO', 'Kodex (sufijo X en serial 353)') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000352', '0000353');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000085', 'VENTA', 'PAGADO', 'CARLOS ASTUDILLO') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000163';

    -- Sin número de factura aún pero con pago BNC
    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES (NULL, 'VENTA', 'PAGADO', 'El Castillo - Pago BNC sin número de factura') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BNC');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000148';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES (NULL, 'VENTA', 'PAGADO', 'Prointel - El Castillo - Pago BNC sin número de factura') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco, referencia)
    VALUES (v_id, 'BNC', '19549533545');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000238';

    -- =========================================================================
    -- 2. FACTURAS PAGADAS POR BINANCE
    -- =========================================================================

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000018', 'VENTA', 'PAGADO', 'Kinpe') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BINANCE');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000025', '0000043', '0000046', '0000049', '0000064',
        '0000066', '0000067', '0000068', '0000069'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000019', 'VENTA', 'PAGADO', 'Comarcas (Binance)') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BINANCE');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000071', '0000075', '0000076', '0000078', '0000079'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000029', 'VENTA', 'PAGADO', 'Kinpe') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BINANCE');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000041', '0000070', '0000073', '0000074'
    );

    -- =========================================================================
    -- 3. FACTURAS / OPERACIONES PAGADAS POR ZELLE
    -- =========================================================================

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000006', 'VENTA', 'PAGADO', 'WILLI') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'ZELLE');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000026';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000007', 'VENTA', 'PAGADO', 'WILLI') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'ZELLE');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000029';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES (NULL, 'VENTA', 'PAGADO', 'Multi Equipos - Pago Zelle sin número de factura') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'ZELLE');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000173';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES (NULL, 'VENTA', 'PAGADO', 'Asor Services - Pago Zelle sin número de factura') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'ZELLE');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000281', '0000282', '0000283');

    -- =========================================================================
    -- 4. FACTURAS PAGADAS EN DÓLARES EFECTIVO
    -- =========================================================================

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000020', 'VENTA', 'PAGADO', 'Multi Equipos (serial 50: CUENTA EDGAR)') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco, cuenta_destino)
    VALUES (v_id, 'DOLARES_EFECTIVO', 'CUENTA EDGAR');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000050', '0000054', '0000099', '0000100', '0000101',
        '0000102', '0000103', '0000104', '0000105', '0000106'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000030', 'VENTA', 'PAGADO', 'Kodex') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'DOLARES_EFECTIVO');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000108', '0000115');

    -- Factura 0000067 (incluye pago USD de 600.00 en 155 y consignación en 175)
    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000067', 'VENTA', 'PAGADO', 'STC (incluye pago USD 600.00 en serial 155 y consignación en serial 175)') RETURNING id INTO v_factura_67;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco, monto)
    VALUES (v_factura_67, 'DOLARES_EFECTIVO', 600.00);
    UPDATE impresoras SET id_factura_comercial = v_factura_67 WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000118', '0000122', '0000132', '0000155', '0000175', '0000260'
    );

    -- =========================================================================
    -- 5. FACTURAS PAGADAS POR BCV
    -- =========================================================================

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000040', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    INSERT INTO pagos_comerciales (id_factura_comercial, metodo_banco) VALUES (v_id, 'BCV');
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000107';

    -- =========================================================================
    -- 6. FACTURAS EN CONSIGNACIÓN
    -- =========================================================================

    INSERT INTO facturas_comerciales (numero_factura, sufijo_control, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000100', 'X', 'CONSIGNACION', 'PENDIENTE_PAGO', 'Comarcas') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000080', '0000081', '0000082', '0000084', '0000085'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES (NULL, 'CONSIGNACION', 'PENDIENTE_PAGO', 'Kodex - Consignación sin factura propia') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000151';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, notas_comerciales)
    VALUES (NULL, 'CONSIGNACION', 'PENDIENTE_PAGO', 'Prointel - El Castillo - Consignación sin factura propia') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000230';

    -- =========================================================================
    -- 7. RESTO DE FACTURAS EMITIDAS (SIN BANCO ESPECIFICADO EN LA HOJA)
    -- =========================================================================

    INSERT INTO facturas_comerciales (numero_factura, sufijo_control, modalidad, estado_cobro)
    VALUES ('0000008', '?', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000051', '0000052', '0000056');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000025', 'VENTA', 'PAGADO', 'PROMOTOR D3XD') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000114';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000027', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000119';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000032', 'VENTA', 'PAGADO', 'PROMOTOR D3XD') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000117';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000036', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000120';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000038', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000149', '0000150');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000039', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000152', '0000153');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000045', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000142', '0000143');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000047', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000177';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000050', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000096', '0000124', '0000125', '0000126', '0000127', '0000128', '0000129', '0000130', '0000131',
        '0000134', '0000135', '0000136', '0000137', '0000138', '0000139'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000051', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000088', '0000098', '0000116');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000054', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000168';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000055', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000207';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000056', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000189';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor, notas_comerciales)
    VALUES ('0000057', 'VENTA', 'PAGADO', 'ALBERT / BRIAN', 'Cliente: Cauchera Orinoco') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000206';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000061', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000208', '0000209', '0000210', '0000211', '0000212', '0000213', '0000214', '0000215'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000068', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000140', '0000141', '0000144', '0000145', '0000146',
        '0000156', '0000157', '0000158', '0000159', '0000160', '0000161'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor, notas_comerciales)
    VALUES ('0000070', 'VENTA', 'PAGADO', 'ARTURO', 'Cliente: Totos') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000268';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000071', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000312', '0000314');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro, promotor_vendedor)
    VALUES ('0000072', 'VENTA', 'PAGADO', 'CARLOS ASTUDILLO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000340', '0000341');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000076', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000289', '0000292', '0000293');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000080', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000290', '0000291', '0000295', '0000296');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000081', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000344';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000082', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000431', '0000432', '0000433', '0000434', '0000435', '0000436', '0000437', '0000438'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000084', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000301';

    INSERT INTO facturas_comerciales (numero_factura, sufijo_control, modalidad, estado_cobro)
    VALUES ('0000085', 'Z', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000147', '0000162', '0000174', '0000188',
        '0000190', '0000191', '0000192', '0000193',
        '0000199', '0000200', '0000201', '0000202', '0000203', '0000204', '0000205',
        '0000310', '0000311', '0000313',
        '0000316', '0000317', '0000318', '0000319', '0000320', '0000321'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000086', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000298';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000088', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000351';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000089', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000288', '0000322', '0000332', '0000336');

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000090', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000443';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000091', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000302';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000092', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000263';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000093', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000412', '0000413', '0000414', '0000415', '0000416', '0000417', '0000418', '0000419'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000094', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000381';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000095', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000380';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000098', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN (
        '0000333', '0000447', '0000448', '0000449', '0000450', '0000451', '0000452', '0000453'
    );

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000101', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000391';

    INSERT INTO facturas_comerciales (numero_factura, modalidad, estado_cobro)
    VALUES ('0000102', 'VENTA', 'PAGADO') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000401';

    INSERT INTO facturas_comerciales (numero_factura, sufijo_control, modalidad, estado_cobro, notas_comerciales)
    VALUES ('0000179', 'X', 'VENTA', 'PAGADO', 'Sufijo X en seriales 184 y 187') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000179', '0000184', '0000186', '0000187');

    -- =========================================================================
    -- 8. CASOS CON DEUDA, RETENCIONES, POR FACTURAR Y DONACIONES
    -- =========================================================================

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, notas_comerciales)
    VALUES ('VENTA', 'DEBE_IVA', 'STC - Debe el IVA') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) IN ('0000355', '0000356');

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, notas_comerciales)
    VALUES ('VENTA', 'DEBE_IVA', 'STC - DEBE EL IVA de 2 Maquinas a 500') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000405';

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, notas_comerciales)
    VALUES ('VENTA', 'PAGO_PARCIAL', 'AEG - Debe 200 AUN DE LA MAQUINA') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000195';

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, notas_comerciales)
    VALUES ('VENTA', 'PAGO_PARCIAL', 'Comarcas - Tienen Deuda') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000354';

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, notas_comerciales)
    VALUES ('VENTA', 'PAGO_PARCIAL', 'Gesco - Llevar la factura cobrar las retenciones') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000420';

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, notas_comerciales)
    VALUES ('VENTA', 'POR_FACTURAR', 'HERMANO BLADIMIR SE DEBE FACTURAR') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000194';

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, promotor_vendedor, notas_comerciales)
    VALUES ('VENTA', 'POR_FACTURAR', 'MAX', 'Vendida a MAX SE DEBE FACTURAR') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000198';

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, notas_comerciales)
    VALUES ('DONACION_REGALO', 'SIN_CARGO', 'REGALO WILLY') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000154';

    INSERT INTO facturas_comerciales (modalidad, estado_cobro, notas_comerciales)
    VALUES ('DONACION_REGALO', 'SIN_CARGO', 'Regalada a la señora Rocio') RETURNING id INTO v_id;
    UPDATE impresoras SET id_factura_comercial = v_id WHERE RIGHT(TRIM(serial_fiscal), 7) = '0000241';
END $$;
