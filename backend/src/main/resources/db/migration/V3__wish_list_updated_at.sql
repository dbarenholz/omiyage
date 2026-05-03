ALTER TABLE wish_lists
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE wish_lists
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE wish_lists
    ALTER COLUMN updated_at SET NOT NULL;
