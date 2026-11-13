-- V63: Control de cajas de empaque/inventario para impresoras fiscales (3FN) y mapeo inicial de seriales.

CREATE TABLE IF NOT EXISTS public.cajas_inventario (
    id            BIGSERIAL PRIMARY KEY,
    numero_caja   INTEGER NOT NULL UNIQUE,
    observaciones VARCHAR(255),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE public.impresoras
    ADD COLUMN IF NOT EXISTS id_caja BIGINT REFERENCES public.cajas_inventario(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_impresoras_id_caja
    ON public.impresoras (id_caja);

INSERT INTO public.cajas_inventario (numero_caja)
SELECT generate_series(1, 83)
ON CONFLICT (numero_caja) DO NOTHING;

WITH rangos_cajas (numero_caja, serial_inicio, serial_fin) AS (
    VALUES
        (1, 20, 27), (2, 28, 35), (3, 36, 43), (4, 44, 51), (5, 52, 59),
        (6, 60, 67), (7, 68, 75), (8, 76, 83), (9, 84, 91), (10, 92, 99),
        (11, 100, 107), (12, 108, 115), (13, 116, 123), (14, 124, 131), (15, 132, 139),
        (16, 140, 147), (17, 148, 155), (18, 156, 163), (19, 164, 171), (20, 172, 179),
        (21, 180, 187), (22, 188, 195), (23, 196, 203), (24, 204, 211), (25, 212, 219),
        (26, 220, 227), (27, 228, 235), (28, 236, 243), (29, 244, 251), (30, 252, 259),
        (31, 260, 267), (32, 268, 275), (33, 276, 283), (34, 284, 291), (35, 292, 299),
        (36, 300, 307), (37, 308, 315), (38, 316, 323), (39, 324, 331), (40, 332, 339),
        (41, 340, 347), (42, 348, 355), (43, 356, 363), (44, 364, 371), (45, 372, 379),
        (46, 380, 387), (47, 388, 395), (48, 396, 403), (49, 404, 411), (50, 412, 419),
        (51, 420, 427), (52, 428, 435), (53, 436, 443), (54, 444, 451), (55, 452, 459),
        (56, 460, 467), (57, 468, 475), (58, 476, 483), (59, 484, 491), (60, 492, 499),
        (61, 500, 507), (62, 508, 515), (63, 516, 523), (64, 524, 531), (65, 532, 539),
        (66, 540, 547), (67, 548, 555), (68, 556, 563), (69, 564, 571), (70, 572, 579),
        (71, 580, 587), (72, 588, 595), (73, 596, 603), (74, 604, 611), (75, 612, 619),
        (76, 620, 627), (77, 628, 635), (78, 636, 643), (79, 644, 651), (80, 652, 659),
        (81, 660, 667), (82, 668, 675), (83, 676, 723)
),
mapa_serial_caja AS (
    SELECT
        c.id AS id_caja,
        LPAD(s::text, 7, '0') AS serial_7_digitos
    FROM rangos_cajas r
    JOIN public.cajas_inventario c ON c.numero_caja = r.numero_caja
    CROSS JOIN LATERAL generate_series(r.serial_inicio, r.serial_fin) AS s
)
UPDATE public.impresoras i
SET id_caja = m.id_caja
FROM mapa_serial_caja m
WHERE RIGHT(TRIM(i.serial_fiscal), 7) = m.serial_7_digitos
  AND i.id_caja IS DISTINCT FROM m.id_caja;
