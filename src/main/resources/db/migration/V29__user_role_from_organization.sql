-- User role follows organization assignment: SC staff become TECHNICIAN; legacy signers become DISTRIBUTOR.

UPDATE public.users
SET role = 'TECHNICIAN'
WHERE role = 'SERVICE_CENTER';

UPDATE public.users
SET role = 'DISTRIBUTOR',
    branch_id = NULL
WHERE role = 'TECHNICIAN'
  AND distributor_id IS NOT NULL;
