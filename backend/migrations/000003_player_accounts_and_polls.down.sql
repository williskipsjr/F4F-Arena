DROP TABLE IF EXISTS match_votes;

ALTER TABLE matches
    DROP COLUMN IF EXISTS is_poll_open,
    DROP COLUMN IF EXISTS kickoff_time;

DROP TABLE IF EXISTS player_accounts;
