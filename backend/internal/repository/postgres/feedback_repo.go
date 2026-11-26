package postgres

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

type FeedbackItem struct {
	ID         string    `json:"id"`
	PlayerID   *string   `json:"player_id,omitempty"`
	AuthorName string    `json:"author_name"`
	Category   string    `json:"category"`
	Message    string    `json:"message"`
	CreatedAt  time.Time `json:"created_at"`
}

type FeedbackRepository struct {
	pool *pgxpool.Pool
}

func NewFeedbackRepository(pool *pgxpool.Pool) *FeedbackRepository {
	return &FeedbackRepository{pool: pool}
}

func (r *FeedbackRepository) Create(ctx context.Context, playerID *string, authorName, category, message string) (*FeedbackItem, error) {
	cat := strings.ToUpper(strings.TrimSpace(category))
	if cat == "" {
		cat = "FEATURE"
	}
	msg := strings.TrimSpace(message)
	if msg == "" {
		return nil, fmt.Errorf("feedback message cannot be empty")
	}
	author := strings.TrimSpace(authorName)
	if author == "" {
		author = "Community Member"
	}

	var item FeedbackItem
	err := r.pool.QueryRow(ctx, `
		INSERT INTO app_feedback (player_id, author_name, category, message, created_at)
		VALUES ($1, $2, $3, $4, NOW())
		RETURNING id, player_id, author_name, category, message, created_at
	`, playerID, author, cat, msg).Scan(
		&item.ID,
		&item.PlayerID,
		&item.AuthorName,
		&item.Category,
		&item.Message,
		&item.CreatedAt,
	)
	if err != nil {
		return nil, fmt.Errorf("failed saving feedback: %w", err)
	}
	return &item, nil
}

func (r *FeedbackRepository) List(ctx context.Context, limit int) ([]FeedbackItem, error) {
	if limit <= 0 || limit > 100 {
		limit = 30
	}
	rows, err := r.pool.Query(ctx, `
		SELECT id, player_id, author_name, category, message, created_at
		FROM app_feedback
		ORDER BY created_at DESC
		LIMIT $1
	`, limit)
	if err != nil {
		return nil, fmt.Errorf("failed listing feedback: %w", err)
	}
	defer rows.Close()

	items := make([]FeedbackItem, 0)
	for rows.Next() {
		var item FeedbackItem
		if err := rows.Scan(&item.ID, &item.PlayerID, &item.AuthorName, &item.Category, &item.Message, &item.CreatedAt); err != nil {
			return nil, fmt.Errorf("failed scanning feedback: %w", err)
		}
		items = append(items, item)
	}
	return items, nil
}
