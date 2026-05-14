DO $$ 
DECLARE 
    t RECORD;
BEGIN 
    FOR t IN 
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
    LOOP
        EXECUTE format(
            'SELECT setval(pg_get_serial_sequence(%L, ''id''), COALESCE(MAX(id), 0) + 1, false) FROM %I', 
            'public.' || t.table_name, 
            t.table_name
        );
    EXCEPTION WHEN OTHERS THEN 
        -- Ignorar tablas que no tienen una columna 'id' o que no usan secuencias
    END LOOP;
END $$;
