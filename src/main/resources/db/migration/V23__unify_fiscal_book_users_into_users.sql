-- Unify fiscal book accounts into users and drop fiscal_book_users.

INSERT INTO users (username, name, password, role, branch_id, distributor_id, enabled)
SELECT f.username,
       f.name,
       f.password,
       'SENIAT',
       NULL,
       NULL,
       f.enabled
FROM fiscal_book_users f
WHERE f.role = 'FISCAL_AUDITOR'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.username = f.username);

INSERT INTO users (username, name, password, role, branch_id, distributor_id, enabled)
SELECT f.username,
       f.name,
       f.password,
       'ADMIN',
       NULL,
       NULL,
       f.enabled
FROM fiscal_book_users f
WHERE f.role = 'FISCAL_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.username = f.username);

INSERT INTO users (username, name, password, role, branch_id, distributor_id, enabled)
SELECT f.username,
       f.name,
       f.password,
       CASE
           WHEN EXISTS (
               SELECT 1
               FROM centros_servicio cs
               WHERE cs.id_sucursal = e.id_sucursal
           ) THEN 'SERVICE_CENTER'
           ELSE 'TECHNICIAN'
       END,
       e.id_sucursal,
       NULL,
       f.enabled
FROM fiscal_book_users f
         INNER JOIN empleados e ON e.id = f.employee_id
WHERE f.role = 'FISCAL_TECHNICIAN'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.username = f.username);

DROP TABLE IF EXISTS fiscal_book_users;
