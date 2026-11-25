package postgres

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

var (
	ErrMatchNotFound = domain.ErrMatchNotFound
)

type MatchRepository struct {
	pool *pgxpool.Pool
}

func NewMatchRepository(pool *pgxpool.Pool) *MatchRepository {
	return &MatchRepository{pool: pool}
}

func (r *MatchRepository) Create(ctx context.Context, m *domain.Match) error {
	query := `
		INSERT INTO matches (title, match_date, kickoff_time, venue, format, status, sub_policy, is_poll_open, notes, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
		RETURNING id, created_at, updated_at
	`
	now := time.Now().UTC()
	err := r.pool.QueryRow(
		ctx,
		query,
		m.Title,
		m.MatchDate,
		m.KickoffTime,
		m.Venue,
		string(m.Format),
		string(m.Status),
		string(m.SubPolicy),
		m.IsPollOpen,
		m.Notes,
		now,
		now,
	).Scan(&m.ID, &m.CreatedAt, &m.UpdatedAt)

	if err != nil {
		return fmt.Errorf("failed inserting match: %w", err)
	}
	return nil
}

func (r *MatchRepository) GetByID(ctx context.Context, id string) (*domain.Match, error) {
	query := `
		SELECT m.id, m.title, m.match_date, m.kickoff_time, m.venue, m.format, m.status, m.sub_policy, m.is_poll_open,
		       COALESCE((SELECT COUNT(*) FROM match_votes v WHERE v.match_id = m.id AND v.vote = 'IN'), 0),
		       COALESCE((SELECT COUNT(*) FROM match_votes v WHERE v.match_id = m.id AND v.vote = 'OUT'), 0),
		       m.notes, m.created_at, m.updated_at
		FROM matches m
		WHERE m.id = $1
	`
	var m domain.Match
	var fmtStr, statusStr, subStr string
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&m.ID,
		&m.Title,
		&m.MatchDate,
		&m.KickoffTime,
		&m.Venue,
		&fmtStr,
		&statusStr,
		&subStr,
		&m.IsPollOpen,
		&m.InCount,
		&m.OutCount,
		&m.Notes,
		&m.CreatedAt,
		&m.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrMatchNotFound
		}
		return nil, fmt.Errorf("failed fetching match: %w", err)
	}
	m.Format = domain.MatchFormat(fmtStr)
	m.Status = domain.MatchStatus(statusStr)
	m.SubPolicy = domain.SubstitutePolicy(subStr)
	return &m, nil
}

func (r *MatchRepository) List(ctx context.Context, status *domain.MatchStatus) ([]domain.Match, error) {
	queryBuilder := strings.Builder{}
	queryBuilder.WriteString(`
		SELECT m.id, m.title, m.match_date, m.kickoff_time, m.venue, m.format, m.status, m.sub_policy, m.is_poll_open,
		       COALESCE((SELECT COUNT(*) FROM match_votes v WHERE v.match_id = m.id AND v.vote = 'IN'), 0),
		       COALESCE((SELECT COUNT(*) FROM match_votes v WHERE v.match_id = m.id AND v.vote = 'OUT'), 0),
		       m.notes, m.created_at, m.updated_at
		FROM matches m
	`)
	var args []interface{}
	if status != nil {
		queryBuilder.WriteString(" WHERE m.status = $1")
		args = append(args, string(*status))
	}
	queryBuilder.WriteString(" ORDER BY m.created_at DESC")

	rows, err := r.pool.Query(ctx, queryBuilder.String(), args...)
	if err != nil {
		return nil, fmt.Errorf("failed listing matches: %w", err)
	}
	defer rows.Close()

	var matches []domain.Match
	for rows.Next() {
		var m domain.Match
		var fmtStr, statusStr, subStr string
		err := rows.Scan(
			&m.ID,
			&m.Title,
			&m.MatchDate,
			&m.KickoffTime,
			&m.Venue,
			&fmtStr,
			&statusStr,
			&subStr,
			&m.IsPollOpen,
			&m.InCount,
			&m.OutCount,
			&m.Notes,
			&m.CreatedAt,
			&m.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed scanning match: %w", err)
		}
		m.Format = domain.MatchFormat(fmtStr)
		m.Status = domain.MatchStatus(statusStr)
		m.SubPolicy = domain.SubstitutePolicy(subStr)
		matches = append(matches, m)
	}

	return matches, nil
}

func (r *MatchRepository) Update(ctx context.Context, m *domain.Match) error {
	query := `
		UPDATE matches
		SET title = $1, match_date = $2, kickoff_time = $3, venue = $4, format = $5, status = $6, sub_policy = $7, is_poll_open = $8, notes = $9, updated_at = $10
		WHERE id = $11
		RETURNING updated_at
	`
	now := time.Now().UTC()
	err := r.pool.QueryRow(
		ctx,
		query,
		m.Title,
		m.MatchDate,
		m.KickoffTime,
		m.Venue,
		string(m.Format),
		string(m.Status),
		string(m.SubPolicy),
		m.IsPollOpen,
		m.Notes,
		now,
		m.ID,
	).Scan(&m.UpdatedAt)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ErrMatchNotFound
		}
		return fmt.Errorf("failed updating match: %w", err)
	}
	return nil
}

func (r *MatchRepository) SyncAttendees(ctx context.Context, matchID string, playerIDs []string) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	// Clear existing attendees
	if _, err := tx.Exec(ctx, "DELETE FROM match_attendees WHERE match_id = $1", matchID); err != nil {
		return fmt.Errorf("failed deleting old attendees: %w", err)
	}

	// Batch insert new attendees
	if len(playerIDs) > 0 {
		batch := &pgx.Batch{}
		now := time.Now().UTC()
		for _, pid := range playerIDs {
			batch.Queue(`
				INSERT INTO match_attendees (match_id, player_id, is_late_arrival, checked_in_at)
				VALUES ($1, $2, false, $3)
				ON CONFLICT (match_id, player_id) DO NOTHING
			`, matchID, pid, now)
		}

		br := tx.SendBatch(ctx, batch)
		for i := 0; i < len(playerIDs); i++ {
			if _, err := br.Exec(); err != nil {
				_ = br.Close()
				return fmt.Errorf("failed inserting attendee: %w", err)
			}
		}
		if err := br.Close(); err != nil {
			return fmt.Errorf("failed closing batch: %w", err)
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("failed to commit attendee sync: %w", err)
	}
	return nil
}

func (r *MatchRepository) GetAttendees(ctx context.Context, matchID string) ([]domain.MatchAttendee, error) {
	query := `
		SELECT 
			ma.match_id, ma.player_id, ma.is_late_arrival, ma.checked_in_at,
			p.id, p.display_name, p.primary_position, p.is_active, p.notes, p.created_at, p.updated_at
		FROM match_attendees ma
		JOIN players p ON ma.player_id = p.id
		WHERE ma.match_id = $1
		ORDER BY p.primary_position ASC, p.display_name ASC
	`
	rows, err := r.pool.Query(ctx, query, matchID)
	if err != nil {
		return nil, fmt.Errorf("failed querying match attendees: %w", err)
	}
	defer rows.Close()

	var attendees []domain.MatchAttendee
	for rows.Next() {
		var a domain.MatchAttendee
		var posStr string
		err := rows.Scan(
			&a.MatchID,
			&a.PlayerID,
			&a.IsLateArrival,
			&a.CheckedInAt,
			&a.Player.ID,
			&a.Player.DisplayName,
			&posStr,
			&a.Player.IsActive,
			&a.Player.Notes,
			&a.Player.CreatedAt,
			&a.Player.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed scanning attendee: %w", err)
		}
		a.Player.PrimaryPosition = domain.Position(posStr)
		attendees = append(attendees, a)
	}

	return attendees, nil
}

func (r *MatchRepository) AddLateAttendee(ctx context.Context, matchID string, playerID string) error {
	query := `
		INSERT INTO match_attendees (match_id, player_id, is_late_arrival, checked_in_at)
		VALUES ($1, $2, true, $3)
		ON CONFLICT (match_id, player_id) DO UPDATE SET is_late_arrival = true
	`
	_, err := r.pool.Exec(ctx, query, matchID, playerID, time.Now().UTC())
	if err != nil {
		return fmt.Errorf("failed adding late attendee: %w", err)
	}
	return nil
}

func (r *MatchRepository) RemoveAttendee(ctx context.Context, matchID string, playerID string) error {
	query := "DELETE FROM match_attendees WHERE match_id = $1 AND player_id = $2"
	res, err := r.pool.Exec(ctx, query, matchID, playerID)
	if err != nil {
		return fmt.Errorf("failed removing attendee: %w", err)
	}
	if res.RowsAffected() == 0 {
		return errors.New("attendee not found in this match")
	}
	return nil
}

// CastVote records a player's IN/OUT vote for a match and keeps match_attendees in sync.
func (r *MatchRepository) CastVote(ctx context.Context, matchID, playerID, vote string) (*domain.Match, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed starting vote transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	var isPollOpen bool
	err = tx.QueryRow(ctx, "SELECT is_poll_open FROM matches WHERE id = $1", matchID).Scan(&isPollOpen)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrMatchNotFound
		}
		return nil, fmt.Errorf("failed checking match poll: %w", err)
	}

	now := time.Now().UTC()
	_, err = tx.Exec(ctx, `
		INSERT INTO match_votes (match_id, player_id, vote, voted_at)
		VALUES ($1, $2, $3, $4)
		ON CONFLICT (match_id, player_id)
		DO UPDATE SET vote = EXCLUDED.vote, voted_at = EXCLUDED.voted_at
	`, matchID, playerID, vote, now)
	if err != nil {
		return nil, fmt.Errorf("failed recording vote: %w", err)
	}

	if vote == "IN" {
		_, err = tx.Exec(ctx, `
			INSERT INTO match_attendees (match_id, player_id, is_late_arrival, checked_in_at)
			VALUES ($1, $2, false, $3)
			ON CONFLICT (match_id, player_id) DO NOTHING
		`, matchID, playerID, now)
		if err != nil {
			return nil, fmt.Errorf("failed adding attendee from IN vote: %w", err)
		}
	} else {
		_, err = tx.Exec(ctx, `
			DELETE FROM match_attendees WHERE match_id = $1 AND player_id = $2
		`, matchID, playerID)
		if err != nil {
			return nil, fmt.Errorf("failed removing attendee from OUT vote: %w", err)
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return nil, fmt.Errorf("failed committing vote: %w", err)
	}

	m, err := r.GetByID(ctx, matchID)
	if err != nil {
		return nil, err
	}
	v := vote
	m.MyVote = &v
	return m, nil
}

// GetPlayerVotesMap returns a map of matchID -> vote ("IN" / "OUT") for a player.
func (r *MatchRepository) GetPlayerVotesMap(ctx context.Context, playerID string) (map[string]string, error) {
	rows, err := r.pool.Query(ctx, "SELECT match_id, vote FROM match_votes WHERE player_id = $1", playerID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	result := make(map[string]string)
	for rows.Next() {
		var mID, vote string
		if err := rows.Scan(&mID, &vote); err == nil {
			result[mID] = vote
		}
	}
	return result, nil
}

// GetMatchVotes returns all IN/OUT votes for a match with player details and vote timestamps.
func (r *MatchRepository) GetMatchVotes(ctx context.Context, matchID string) ([]domain.MatchVoteDetail, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT v.player_id, p.display_name, p.primary_position, v.vote, v.voted_at
		FROM match_votes v
		JOIN players p ON p.id = v.player_id
		WHERE v.match_id = $1
		ORDER BY v.vote ASC, v.voted_at DESC
	`, matchID)
	if err != nil {
		return nil, fmt.Errorf("failed fetching match votes: %w", err)
	}
	defer rows.Close()

	votes := make([]domain.MatchVoteDetail, 0)
	for rows.Next() {
		var item domain.MatchVoteDetail
		var posStr string
		if err := rows.Scan(&item.PlayerID, &item.DisplayName, &posStr, &item.Vote, &item.VotedAt); err != nil {
			return nil, fmt.Errorf("failed scanning match vote: %w", err)
		}
		item.PrimaryPosition = domain.Position(posStr)
		votes = append(votes, item)
	}
	return votes, nil
}

// EnsureAdminPlayerID resolves or creates a player profile for the admin account.
func (r *MatchRepository) EnsureAdminPlayerID(ctx context.Context, adminName string) (string, error) {
	name := strings.TrimSpace(adminName)
	if name == "" {
		name = "Admin"
	}
	var playerID string
	err := r.pool.QueryRow(ctx, `
		SELECT id FROM players WHERE LOWER(display_name) = LOWER($1) LIMIT 1
	`, name).Scan(&playerID)
	if err == nil {
		return playerID, nil
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return "", err
	}

	now := time.Now().UTC()
	err = r.pool.QueryRow(ctx, `
		INSERT INTO players (display_name, primary_position, is_active, notes, created_at, updated_at)
		VALUES ($1, 'MIDFIELDER', true, 'Community Admin', $2, $2)
		RETURNING id
	`, name, now).Scan(&playerID)
	if err != nil {
		return "", err
	}
	return playerID, nil
}
