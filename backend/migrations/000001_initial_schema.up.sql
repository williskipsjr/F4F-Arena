-- F4F Arena — Initial Schema Migration
-- Migration: 000001_initial_schema.up.sql

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Custom Enum Types
CREATE TYPE player_position AS ENUM (
    'KEEPER',
    'DEFENDER',
    'MIDFIELDER',
    'STRIKER'
);

CREATE TYPE match_format AS ENUM (
    'SIX_A_SIDE',
    'SEVEN_A_SIDE',
    'NINE_A_SIDE'
);

CREATE TYPE match_status AS ENUM (
    'DRAFT',
    'ATTENDANCE_LOCKED',
    'TEAMS_GENERATED',
    'FINALISED',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE substitute_policy AS ENUM (
    'STRICT_SUBSTITUTES',
    'DISTRIBUTE_ALL'
);

-- 2. Players Table
CREATE TABLE players (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    display_name VARCHAR(100) NOT NULL,
    primary_position player_position NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_players_active_position ON players (is_active, primary_position);
CREATE INDEX idx_players_display_name ON players (display_name);

-- 3. Matches Table
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(150),
    match_date TIMESTAMPTZ,
    venue VARCHAR(150),
    format match_format NOT NULL DEFAULT 'SEVEN_A_SIDE',
    status match_status NOT NULL DEFAULT 'DRAFT',
    sub_policy substitute_policy NOT NULL DEFAULT 'STRICT_SUBSTITUTES',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_matches_date_status ON matches (match_date, status);

-- 4. Match Attendees Table
CREATE TABLE match_attendees (
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES players(id) ON DELETE RESTRICT,
    is_late_arrival BOOLEAN NOT NULL DEFAULT FALSE,
    checked_in_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id, player_id)
);

CREATE INDEX idx_match_attendees_player ON match_attendees (player_id);

-- 5. Teams Table
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    team_number INT NOT NULL,
    team_name VARCHAR(50) NOT NULL,
    bib_color VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_match_team_number UNIQUE (match_id, team_number)
);

CREATE INDEX idx_teams_match ON teams (match_id);

-- 6. Team Assignments Table
CREATE TABLE team_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES players(id) ON DELETE RESTRICT,
    assigned_position player_position NOT NULL,
    is_substitute BOOLEAN NOT NULL DEFAULT FALSE,
    is_manual_override BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_match_player_assignment UNIQUE (match_id, player_id)
);

CREATE INDEX idx_assignments_team ON team_assignments (team_id);
CREATE INDEX idx_assignments_match_sub ON team_assignments (match_id, is_substitute);

-- 7. Schema Migrations Audit Table
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(50) PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
