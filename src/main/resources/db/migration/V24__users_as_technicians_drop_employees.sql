-- Unify field technician identity in users; drop empleados/tecnicos/distribuidores (personas).

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS national_id VARCHAR(32);

-- Backfill cédula from empleados matched by email.
UPDATE public.users u
SET national_id = e.cedula
FROM public.empleados e
WHERE u.national_id IS NULL
  AND lower(trim(u.username)) = lower(trim(e.correo));

-- Operational roles collapse to TECHNICIAN.
UPDATE public.users
SET role = 'TECHNICIAN'
WHERE role IN ('DISTRIBUTOR', 'SERVICE_CENTER');

-- Distributor scope for technicians missing distributor_id (branch is distributor HQ).
UPDATE public.users u
SET distributor_id = d.id
FROM public.distribuidoras d
WHERE u.role = 'TECHNICIAN'
  AND u.distributor_id IS NULL
  AND u.branch_id IS NOT NULL
  AND u.branch_id = d.id_sucursal;

-- Service-center branch: infer distributor from most recent technical service on that technician.
UPDATE public.users u
SET distributor_id = inferred.distributor_id
FROM (
    SELECT DISTINCT ON (u2.id)
        u2.id AS user_id,
        st.id_distribuidora AS distributor_id
    FROM public.users u2
    INNER JOIN public.empleados e ON lower(trim(e.correo)) = lower(trim(u2.username))
    INNER JOIN public.tecnicos t ON t.id_empleado = e.id
    INNER JOIN public.servicios_tecnicos st ON st.id_tecnico = t.id
    WHERE st.id_distribuidora IS NOT NULL
    ORDER BY u2.id, st.created_at DESC NULLS LAST
) inferred
WHERE u.id = inferred.user_id
  AND u.role = 'TECHNICIAN'
  AND u.distributor_id IS NULL;

-- Technicians are scoped by distributor, not branch.
UPDATE public.users
SET branch_id = NULL
WHERE role = 'TECHNICIAN';

-- Technical services: link reviewer user.
ALTER TABLE public.servicios_tecnicos ADD COLUMN IF NOT EXISTS id_usuario BIGINT;

UPDATE public.servicios_tecnicos st
SET id_usuario = u.id
FROM public.tecnicos t
INNER JOIN public.empleados e ON e.id = t.id_empleado
INNER JOIN public.users u ON lower(trim(u.username)) = lower(trim(e.correo))
WHERE st.id_tecnico = t.id
  AND st.id_usuario IS NULL;

-- Annual inspections: add user column and backfill before dropping employee FK.
ALTER TABLE public.inspecciones_anuales ADD COLUMN IF NOT EXISTS id_usuario BIGINT;

UPDATE public.inspecciones_anuales ia
SET id_usuario = u.id
FROM public.empleados e
INNER JOIN public.users u ON lower(trim(u.username)) = lower(trim(e.correo))
WHERE ia.id_empleado = e.id
  AND ia.id_usuario IS NULL;

-- Orphan rows: assign first admin user so NOT NULL can be applied (review in prod if any).
UPDATE public.servicios_tecnicos
SET id_usuario = (SELECT id FROM public.users WHERE role = 'ADMIN' ORDER BY id LIMIT 1)
WHERE id_usuario IS NULL;

UPDATE public.inspecciones_anuales
SET id_usuario = (SELECT id FROM public.users WHERE role = 'ADMIN' ORDER BY id LIMIT 1)
WHERE id_usuario IS NULL;

ALTER TABLE public.servicios_tecnicos
    DROP CONSTRAINT IF EXISTS servicios_tecnicos_id_tecnico_fkey;

ALTER TABLE public.servicios_tecnicos DROP COLUMN IF EXISTS id_tecnico;

ALTER TABLE public.servicios_tecnicos
    ADD CONSTRAINT servicios_tecnicos_id_usuario_fkey
        FOREIGN KEY (id_usuario) REFERENCES public.users (id);

ALTER TABLE public.servicios_tecnicos ALTER COLUMN id_usuario SET NOT NULL;

ALTER TABLE public.inspecciones_anuales
    DROP CONSTRAINT IF EXISTS inspecciones_anuales_id_empleado_fkey;

ALTER TABLE public.inspecciones_anuales DROP COLUMN IF EXISTS id_empleado;

ALTER TABLE public.inspecciones_anuales
    ADD CONSTRAINT inspecciones_anuales_id_usuario_fkey
        FOREIGN KEY (id_usuario) REFERENCES public.users (id);

ALTER TABLE public.inspecciones_anuales ALTER COLUMN id_usuario SET NOT NULL;

DELETE FROM public.modification_requests WHERE target_type = 'EMPLOYEE';

DROP TABLE IF EXISTS public.distribuidores;
DROP TABLE IF EXISTS public.tecnicos;
DROP TABLE IF EXISTS public.empleados;

CREATE UNIQUE INDEX IF NOT EXISTS users_national_id_key ON public.users (national_id)
    WHERE national_id IS NOT NULL;
