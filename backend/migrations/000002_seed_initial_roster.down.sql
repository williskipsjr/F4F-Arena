-- F4F Arena — Rollback Initial Roster Seed
-- Migration: 000002_seed_initial_roster.down.sql

DELETE FROM players WHERE display_name IN (
    'Lenpu', 'James Singson', 'Andrew', 'Paogoulen Kilong', 'Jangkholen',
    'Thonglam', 'Robert', 'Mimin Baite', 'Seiminthang', 'Lalchaneo', 'Johny',
    'Paolal', 'D S Thangboi', 'Manglal', 'Bob', 'Seiminlen', 'Ngambom',
    'Thangminlun', 'Peter', 'Zamsei', 'Eric', 'Joshua', 'Mimin Touthang',
    'Paokai', 'Mangrose', 'Boilun', 'Paoneo', 'Manggou', 'Nelson', 'Mynlun',
    'Jack', 'Thangtinlun', 'Mang Doungel', 'Joseph', 'Savio', 'Lalboi',
    'Haopu', 'James Kipgen', 'TL Mang', 'Seilen', 'Tonglen', 'Thangminlen'
);
