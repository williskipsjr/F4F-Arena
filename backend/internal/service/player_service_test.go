package service

import (
	"context"
	"errors"
	"testing"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

type mockPlayerStoreForService struct {
	players map[string]domain.Player
}

func (m *mockPlayerStoreForService) List(ctx context.Context, filter domain.PlayerFilter) ([]domain.Player, error) {
	var list []domain.Player
	for _, p := range m.players {
		if filter.IsActive != nil && p.IsActive != *filter.IsActive {
			continue
		}
		if filter.Position != nil && p.PrimaryPosition != *filter.Position {
			continue
		}
		list = append(list, p)
	}
	return list, nil
}

func (m *mockPlayerStoreForService) GetByID(ctx context.Context, id string) (*domain.Player, error) {
	p, exists := m.players[id]
	if !exists {
		return nil, errors.New("player not found")
	}
	return &p, nil
}

func (m *mockPlayerStoreForService) Create(ctx context.Context, p *domain.Player) error {
	p.ID = "p-new-id"
	m.players[p.ID] = *p
	return nil
}

func (m *mockPlayerStoreForService) Update(ctx context.Context, p *domain.Player) error {
	m.players[p.ID] = *p
	return nil
}

func (m *mockPlayerStoreForService) FindSimilarNames(ctx context.Context, name string, excludeID string) ([]string, error) {
	var matches []string
	for _, p := range m.players {
		if p.DisplayName == name && p.ID != excludeID {
			matches = append(matches, p.DisplayName)
		}
	}
	return matches, nil
}

func TestPlayerService_CreatePlayer(t *testing.T) {
	store := &mockPlayerStoreForService{players: make(map[string]domain.Player)}
	store.players["p1"] = domain.Player{ID: "p1", DisplayName: "Existing Player", PrimaryPosition: domain.PositionDefender}

	svc := NewPlayerService(store)

	// Valid creation
	p, warnings, err := svc.CreatePlayer(context.Background(), domain.CreatePlayerInput{
		DisplayName:     "New Player",
		PrimaryPosition: domain.PositionStriker,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if p.DisplayName != "New Player" {
		t.Errorf("expected 'New Player', got %s", p.DisplayName)
	}
	if len(warnings) != 0 {
		t.Errorf("expected 0 warnings, got %d", len(warnings))
	}

	// Duplicate detection warning
	_, warningsDuplicate, err := svc.CreatePlayer(context.Background(), domain.CreatePlayerInput{
		DisplayName:     "Existing Player",
		PrimaryPosition: domain.PositionStriker,
	})
	if err != nil {
		t.Fatalf("unexpected error on duplicate name: %v", err)
	}
	if len(warningsDuplicate) == 0 {
		t.Errorf("expected duplicate name warning, got 0 warnings")
	}
}

func TestPlayerService_UpdatePlayer(t *testing.T) {
	store := &mockPlayerStoreForService{players: make(map[string]domain.Player)}
	store.players["p1"] = domain.Player{ID: "p1", DisplayName: "Old Name", PrimaryPosition: domain.PositionDefender, IsActive: true}

	svc := NewPlayerService(store)

	newName := "Updated Name"
	newPos := domain.PositionMidfielder
	updated, err := svc.UpdatePlayer(context.Background(), "p1", domain.UpdatePlayerInput{
		DisplayName:     &newName,
		PrimaryPosition: &newPos,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if updated.DisplayName != "Updated Name" || updated.PrimaryPosition != domain.PositionMidfielder {
		t.Errorf("expected updated fields, got %+v", updated)
	}
}
