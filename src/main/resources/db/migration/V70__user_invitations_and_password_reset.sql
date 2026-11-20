-- Invitaciones por correo y restablecimiento de contraseña

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS invitation_pending BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS invited_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS user_action_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    purpose VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_action_tokens_user_purpose
    ON user_action_tokens (user_id, purpose);

CREATE INDEX IF NOT EXISTS idx_user_action_tokens_expires_at
    ON user_action_tokens (expires_at);
