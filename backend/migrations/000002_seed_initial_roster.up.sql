-- F4F Arena — Seed Initial 42-Player Roster
-- Migration: 000002_seed_initial_roster.up.sql

INSERT INTO players (display_name, primary_position, is_active) VALUES
-- Keepers (5)
('Lenpu', 'KEEPER', true),
('James Singson', 'KEEPER', true),
('Andrew', 'KEEPER', true),
('Paogoulen Kilong', 'KEEPER', true),
('Jangkholen', 'KEEPER', true),

-- Defenders (14)
('Thonglam', 'DEFENDER', true),
('Robert', 'DEFENDER', true),
('Mimin Baite', 'DEFENDER', true),
('Seiminthang', 'DEFENDER', true),
('Lalchaneo', 'DEFENDER', true),
('Johny', 'DEFENDER', true),
('Paolal', 'DEFENDER', true),
('D S Thangboi', 'DEFENDER', true),
('Manglal', 'DEFENDER', true),
('Bob', 'DEFENDER', true),
('Seiminlen', 'DEFENDER', true),
('Ngambom', 'DEFENDER', true),
('Thangminlun', 'DEFENDER', true),
('Peter', 'DEFENDER', true),

-- Midfielders (14)
('Zamsei', 'MIDFIELDER', true),
('Eric', 'MIDFIELDER', true),
('Joshua', 'MIDFIELDER', true),
('Mimin Touthang', 'MIDFIELDER', true),
('Paokai', 'MIDFIELDER', true),
('Mangrose', 'MIDFIELDER', true),
('Boilun', 'MIDFIELDER', true),
('Paoneo', 'MIDFIELDER', true),
('Manggou', 'MIDFIELDER', true),
('Nelson', 'MIDFIELDER', true),
('Mynlun', 'MIDFIELDER', true),
('Jack', 'MIDFIELDER', true),
('Thangtinlun', 'MIDFIELDER', true),
('Mang Doungel', 'MIDFIELDER', true),

-- Strikers (9)
('Joseph', 'STRIKER', true),
('Savio', 'STRIKER', true),
('Lalboi', 'STRIKER', true),
('Haopu', 'STRIKER', true),
('James Kipgen', 'STRIKER', true),
('TL Mang', 'STRIKER', true),
('Seilen', 'STRIKER', true),
('Tonglen', 'STRIKER', true),
('Thangminlen', 'STRIKER', true)
ON CONFLICT DO NOTHING;
