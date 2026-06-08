-- Usuarios del portal de libros fiscales (separados de users del panel)
CREATE TABLE fiscal_book_users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    employee_id BIGINT REFERENCES empleados(id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fiscal_book_users_employee ON fiscal_book_users(employee_id);
