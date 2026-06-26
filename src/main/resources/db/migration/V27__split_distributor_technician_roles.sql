-- Split operational TECHNICIAN into DISTRIBUTOR; TECHNICIAN becomes signer-only profile.

UPDATE public.users
SET role = 'DISTRIBUTOR'
WHERE role = 'TECHNICIAN';
