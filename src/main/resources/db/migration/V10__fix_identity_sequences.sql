-- Migración para sincronizar todas las secuencias (identity / serial) con el valor máximo actual de cada tabla.
-- Esto corrige el error 'duplicate key value violates unique constraint' cuando las tablas fueron pobladas manualmente o desde un backup.

DO $$ 
DECLARE 
    r RECORD;
    seq_name text;
    max_val bigint;
BEGIN 
    FOR r IN 
        SELECT table_schema, table_name, column_name 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND (column_default LIKE 'nextval%' 
               OR identity_generation IS NOT NULL)
    LOOP
        -- Obtener el nombre de la secuencia asociada a la columna
        seq_name := pg_get_serial_sequence(r.table_schema || '.' || r.table_name, r.column_name);
        
        IF seq_name IS NOT NULL THEN
            -- Obtener el valor máximo actual de la tabla
            EXECUTE format('SELECT COALESCE(MAX(%I), 0) FROM %I.%I', r.column_name, r.table_schema, r.table_name) INTO max_val;
            
            -- Actualizar la secuencia al valor máximo (o 1 si está vacía)
            EXECUTE format('SELECT setval(%L, %s, true)', seq_name, GREATEST(max_val, 1));
        END IF;
    END LOOP;
END $$;
