-- V1__initial_schema.sql: create all application tables

CREATE TABLE users (
    id             UUID        PRIMARY KEY,
    -- Mullvad-style 16-digit login credential, displayed as XXXX-XXXX-XXXX-XXXX
    account_number BIGINT      NOT NULL UNIQUE,
    display_name   VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Null means the account has never been used (candidate for cleanup)
    last_login_at  TIMESTAMPTZ
);

CREATE TABLE wish_lists (
    id       UUID        PRIMARY KEY,
    owner_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title    VARCHAR(255) NOT NULL,
    description TEXT,
    -- Public share identifier (Google Drive-style secret URL token)
    share_id UUID        NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE wishes (
    id                UUID        PRIMARY KEY,
    list_id           UUID        NOT NULL REFERENCES wish_lists(id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    approximate_price NUMERIC(10, 2),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE wish_links (
    id      UUID    PRIMARY KEY,
    wish_id UUID    NOT NULL REFERENCES wishes(id) ON DELETE CASCADE,
    url     TEXT    NOT NULL,
    label   VARCHAR(255)
);

CREATE TABLE wish_tags (
    id      UUID         PRIMARY KEY,
    wish_id UUID         NOT NULL REFERENCES wishes(id) ON DELETE CASCADE,
    tag     VARCHAR(100) NOT NULL
);

CREATE TABLE wish_claims (
    id            UUID        PRIMARY KEY,
    wish_id       UUID        NOT NULL UNIQUE REFERENCES wishes(id) ON DELETE CASCADE,
    claimed_by_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    claimed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for common query patterns
CREATE INDEX idx_users_account_number    ON users(account_number);
CREATE INDEX idx_wish_lists_owner_id     ON wish_lists(owner_id);
CREATE INDEX idx_wish_lists_share_id     ON wish_lists(share_id);
CREATE INDEX idx_wishes_list_id          ON wishes(list_id);
CREATE INDEX idx_wish_links_wish_id      ON wish_links(wish_id);
CREATE INDEX idx_wish_tags_wish_id       ON wish_tags(wish_id);
CREATE INDEX idx_wish_claims_wish_id     ON wish_claims(wish_id);
