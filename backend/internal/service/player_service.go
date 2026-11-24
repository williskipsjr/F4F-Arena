package service

import (
	"context"
	"fmt"
	"strings"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

type PlayerStore interface {
	List(ctx context.Context, filter domain.PlayerFilter) ([]domain.Player, error)
	GetByID(ctx context.Context, id string) (*domain.Player, error)
	Create(ctx context.Context, player *domain.Player) error
	Update(ctx context.Context, player *domain.Player) error
	FindSimilarNames(ctx context.Context, name string, excludeID string) ([]string, error)
}

type PlayerService struct {
	store PlayerStore
}

func NewPlayerService(store PlayerStore) *PlayerService {
	return &PlayerService{store: store}
}

func (s *PlayerService) ListPlayers(ctx context.Context, filter domain.PlayerFilter) ([]domain.Player, error) {
	return s.store.List(ctx, filter)
}

func (s *PlayerService) GetPlayer(ctx context.Context, id string) (*domain.Player, error) {
	if strings.TrimSpace(id) == "" {
		return nil, fmt.Errorf("invalid player id")
	}
	return s.store.GetByID(ctx, id)
}

func (s *PlayerService) CreatePlayer(ctx context.Context, input domain.CreatePlayerInput) (*domain.Player, []string, error) {
	isActive := true
	if input.IsActive != nil {
		isActive = *input.IsActive
	}

	player := &domain.Player{
		DisplayName:     strings.TrimSpace(input.DisplayName),
		PrimaryPosition: input.PrimaryPosition,
		IsActive:        isActive,
		Notes:           input.Notes,
	}

	if err := player.Validate(); err != nil {
		return nil, nil, err
	}

	// Duplicate detection warning (PRD 02: Flag likely duplicate records)
	var warnings []string
	matches, _ := s.store.FindSimilarNames(ctx, player.DisplayName, "00000000-0000-0000-0000-000000000000")
	if len(matches) > 0 {
		warnings = append(warnings, fmt.Sprintf("Similar player name(s) already exist: %s", strings.Join(matches, ", ")))
	}

	if err := s.store.Create(ctx, player); err != nil {
		return nil, nil, err
	}

	return player, warnings, nil
}

func (s *PlayerService) UpdatePlayer(ctx context.Context, id string, input domain.UpdatePlayerInput) (*domain.Player, error) {
	existing, err := s.store.GetByID(ctx, id)
	if err != nil {
		return nil, err
	}

	if input.DisplayName != nil {
		existing.DisplayName = strings.TrimSpace(*input.DisplayName)
	}
	if input.PrimaryPosition != nil {
		existing.PrimaryPosition = *input.PrimaryPosition
	}
	if input.IsActive != nil {
		existing.IsActive = *input.IsActive
	}
	if input.Notes != nil {
		existing.Notes = input.Notes
	}

	if err := existing.Validate(); err != nil {
		return nil, err
	}

	if err := s.store.Update(ctx, existing); err != nil {
		return nil, err
	}

	return existing, nil
}
