-- V56: Registrar versión legacy 1.0.2 en firmwares y vincular impresoras correspondientes

INSERT INTO public.firmwares (
    version,
    nombre_archivo,
    tamano_bytes,
    checksum_sha256,
    checksum_md5,
    id_modelo_impresora,
    notas,
    created_at
)
SELECT 
    '1.0.2',
    'firmware_V_1_0_2_legacy.bin',
    1031040,
    '0000000000000000000000000000000000000000000000000000000000000000',
    '00000000000000000000000000000000',
    NULL,
    'Versión legacy histórica 1.0.2 para reconciliación de impresoras en campo',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.firmwares WHERE version = '1.0.2'
);
