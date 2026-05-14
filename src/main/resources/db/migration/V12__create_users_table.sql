-- V12: Creación de tabla de usuarios para RBAC
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insertar el administrador inicial (la contraseña es aeg-r1 cifrada con BCrypt)
-- IMPORTANTE: Esta es la misma contraseña que ya usabas, pero ahora en BD.
INSERT INTO users (username, password, role) 
VALUES ('segar12345@gmail.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uyQ6Z7.L6TqQZ/uIqS7S7/v0Tj8J1LTC', 'ADMIN');
