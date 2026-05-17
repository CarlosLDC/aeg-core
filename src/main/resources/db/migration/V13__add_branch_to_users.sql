-- V13: Add optional branch FK to users
ALTER TABLE users
    ADD COLUMN branch_id BIGINT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_branch
    FOREIGN KEY (branch_id)
    REFERENCES sucursales(id)
    ON DELETE SET NULL;

CREATE INDEX idx_users_branch_id
    ON users(branch_id);
