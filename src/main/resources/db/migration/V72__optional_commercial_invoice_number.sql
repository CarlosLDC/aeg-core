-- V72: El número de orden lo completa Finanzas después de la asignación.
ALTER TABLE public.facturas_comerciales
    ALTER COLUMN numero_factura DROP NOT NULL;
