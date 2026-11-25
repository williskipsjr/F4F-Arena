-- Player accounts for community sign-up/sign-in and match poll voting
CREATE TABLE IF NOT EXISTS player_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL UNIQUE REFERENCES players(id) ON DELETE CASCADE,
    username TEXT NOT NULL UNIQUE,
    phone TEXT,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'PLAYER' CHECK (role IN ('PLAYER', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_player_accounts_username ON player_accounts(username);

-- Add poll fields to matches
ALTER TABLE matches
    ADD COLUMN IF NOT EXISTS kickoff_time TEXT,
    ADD COLUMN IF NOT EXISTS is_poll_open BOOLEAN NOT NULL DEFAULT FALSE;

-- Match votes (IN / OUT) per player
CREATE TABLE IF NOT EXISTS match_votes (
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    vote TEXT NOT NULL CHECK (vote IN ('IN', 'OUT')),
    voted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (match_id, player_id)
);

CREATE INDEX IF NOT EXISTS idx_match_votes_match_id ON match_votes(match_id);
