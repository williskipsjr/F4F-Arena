-- F4F Arena — Rollback Initial Schema Migration
-- Migration: 000001_initial_schema.down.sql

DROP TABLE IF EXISTS team_assignments CASCADE;
DROP TABLE IF EXISTS teams CASCADE;
DROP TABLE IF EXISTS match_attendees CASCADE;
DROP TABLE IF EXISTS matches CASCADE;
DROP TABLE IF EXISTS players CASCADE;
DROP TABLE IF EXISTS schema_migrations CASCADE;

DROP TYPE IF EXISTS substitute_policy;
DROP TYPE IF EXISTS match_status;
DROP TYPE IF EXISTS match_format;
DROP TYPE IF EXISTS player_position;
