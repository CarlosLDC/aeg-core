-- Migración para arreglar el tipo de la columna "tipo" en empleados
-- Hibernate inserta VARCHAR (text), por lo que debemos cambiar el enum tipo_empleado a text.

ALTER TABLE public.empleados ALTER COLUMN tipo TYPE text USING tipo::text;
