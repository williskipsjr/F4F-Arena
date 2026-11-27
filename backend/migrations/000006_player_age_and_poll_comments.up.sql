ALTER TABLE players
    ADD COLUMN IF NOT EXISTS age INT;

CREATE TABLE IF NOT EXISTS match_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    player_id UUID REFERENCES players(id) ON DELETE SET NULL,
    author_name TEXT NOT NULL,
    author_role TEXT NOT NULL DEFAULT 'PLAYER',
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_match_comments_match_created
    ON match_comments (match_id, created_at ASC);
