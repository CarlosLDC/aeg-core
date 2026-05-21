-- V17: Add display name to users
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS name VARCHAR(150);

UPDATE public.users
SET name = COALESCE(
    NULLIF(TRIM(name), ''),
    NULLIF(TRIM(split_part(username, '@', 1)), ''),
    username
)
WHERE name IS NULL OR TRIM(name) = '';

ALTER TABLE public.users
    ALTER COLUMN name SET NOT NULL;
