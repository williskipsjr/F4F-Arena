ALTER TABLE matches
    DROP COLUMN IF EXISTS post_match_photo_url,
    DROP COLUMN IF EXISTS fee_per_player,
    DROP COLUMN IF EXISTS turf_fee;

ALTER TABLE players
    DROP COLUMN IF EXISTS pre_phone,
    DROP COLUMN IF EXISTS avatar_emoji;
