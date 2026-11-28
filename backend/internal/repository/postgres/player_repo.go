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
	ErrPlayerNotFound = errors.New("player not found")
)

type PlayerRepository struct {
	pool *pgxpool.Pool
}

func NewPlayerRepository(pool *pgxpool.Pool) *PlayerRepository {
	return &PlayerRepository{pool: pool}
}

func (r *PlayerRepository) List(ctx context.Context, filter domain.PlayerFilter) ([]domain.Player, error) {
	queryBuilder := strings.Builder{}
	queryBuilder.WriteString(`
		SELECT id, display_name, primary_position, age, avatar_emoji, avatar_url, is_active, notes, created_at, updated_at
		FROM players
		WHERE 1=1
	`)

	var args []interface{}
	argIdx := 1

	if filter.IsActive != nil {
		queryBuilder.WriteString(fmt.Sprintf(" AND is_active = $%d", argIdx))
		args = append(args, *filter.IsActive)
		argIdx++
	}

	if filter.Position != nil {
		queryBuilder.WriteString(fmt.Sprintf(" AND primary_position = $%d", argIdx))
		args = append(args, string(*filter.Position))
		argIdx++
	}

	if filter.Query != nil && strings.TrimSpace(*filter.Query) != "" {
		queryBuilder.WriteString(fmt.Sprintf(" AND display_name ILIKE $%d", argIdx))
		args = append(args, "%"+strings.TrimSpace(*filter.Query)+"%")
		argIdx++
	}

	queryBuilder.WriteString(" ORDER BY display_name ASC")

	rows, err := r.pool.Query(ctx, queryBuilder.String(), args...)
	if err != nil {
		return nil, fmt.Errorf("failed querying players: %w", err)
	}
	defer rows.Close()

	var players []domain.Player
	for rows.Next() {
		var p domain.Player
		var posStr string
		err := rows.Scan(
			&p.ID,
			&p.DisplayName,
			&posStr,
			&p.Age,
			&p.AvatarEmoji,
			&p.AvatarURL,
			&p.IsActive,
			&p.Notes,
			&p.CreatedAt,
			&p.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed scanning player: %w", err)
		}
		p.PrimaryPosition = domain.Position(posStr)
		players = append(players, p)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("row iteration error: %w", err)
	}

	return players, nil
}

func (r *PlayerRepository) GetByID(ctx context.Context, id string) (*domain.Player, error) {
	query := `
		SELECT id, display_name, primary_position, age, avatar_emoji, avatar_url, is_active, notes, created_at, updated_at
		FROM players
		WHERE id = $1
	`
	var p domain.Player
	var posStr string
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&p.ID,
		&p.DisplayName,
		&posStr,
		&p.Age,
		&p.AvatarEmoji,
		&p.AvatarURL,
		&p.IsActive,
		&p.Notes,
		&p.CreatedAt,
		&p.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrPlayerNotFound
		}
		return nil, fmt.Errorf("failed fetching player by id: %w", err)
	}
	p.PrimaryPosition = domain.Position(posStr)
	return &p, nil
}

func (r *PlayerRepository) Create(ctx context.Context, p *domain.Player) error {
	if p.AvatarEmoji == "" {
		p.AvatarEmoji = "⚽"
	}
	query := `
		INSERT INTO players (display_name, primary_position, age, avatar_emoji, avatar_url, pre_phone, is_active, notes, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
		RETURNING id, created_at, updated_at
	`
	now := time.Now().UTC()
	err := r.pool.QueryRow(
		ctx,
		query,
		p.DisplayName,
		string(p.PrimaryPosition),
		p.Age,
		p.AvatarEmoji,
		p.AvatarURL,
		p.PrePhone,
		p.IsActive,
		p.Notes,
		now,
		now,
	).Scan(&p.ID, &p.CreatedAt, &p.UpdatedAt)

	if err != nil {
		return fmt.Errorf("failed inserting player: %w", err)
	}
	return nil
}

func (r *PlayerRepository) Update(ctx context.Context, p *domain.Player) error {
	if p.AvatarEmoji == "" {
		p.AvatarEmoji = "⚽"
	}
	query := `
		UPDATE players
		SET display_name = $1, primary_position = $2, age = $3, avatar_emoji = $4, avatar_url = COALESCE($5, avatar_url), pre_phone = COALESCE($6, pre_phone), is_active = $7, notes = $8, updated_at = $9
		WHERE id = $10
		RETURNING updated_at
	`
	now := time.Now().UTC()
	err := r.pool.QueryRow(
		ctx,
		query,
		p.DisplayName,
		string(p.PrimaryPosition),
		p.Age,
		p.AvatarEmoji,
		p.AvatarURL,
		p.PrePhone,
		p.IsActive,
		p.Notes,
		now,
		p.ID,
	).Scan(&p.UpdatedAt)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ErrPlayerNotFound
		}
		return fmt.Errorf("failed updating player: %w", err)
	}
	return nil
}

func (r *PlayerRepository) FindSimilarNames(ctx context.Context, name string, excludeID string) ([]string, error) {
	query := `
		SELECT display_name
		FROM players
		WHERE display_name ILIKE $1 AND id != $2
		LIMIT 5
	`
	rows, err := r.pool.Query(ctx, query, "%"+strings.TrimSpace(name)+"%", excludeID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var matches []string
	for rows.Next() {
		var n string
		if err := rows.Scan(&n); err == nil {
			matches = append(matches, n)
		}
	}
	return matches, nil
}
