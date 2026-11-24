package service

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

type mockMatchStoreForService struct {
	matches   map[string]domain.Match
	attendees map[string][]domain.MatchAttendee
}

func (m *mockMatchStoreForService) Create(ctx context.Context, match *domain.Match) error {
	match.ID = "m-test"
	m.matches[match.ID] = *match
	return nil
}

func (m *mockMatchStoreForService) GetByID(ctx context.Context, id string) (*domain.Match, error) {
	match, exists := m.matches[id]
	if !exists {
		return nil, errors.New("match not found")
	}
	return &match, nil
}

func (m *mockMatchStoreForService) List(ctx context.Context, status *domain.MatchStatus) ([]domain.Match, error) {
	var list []domain.Match
	for _, match := range m.matches {
		list = append(list, match)
	}
	return list, nil
}

func (m *mockMatchStoreForService) Update(ctx context.Context, match *domain.Match) error {
	m.matches[match.ID] = *match
	return nil
}

func (m *mockMatchStoreForService) SyncAttendees(ctx context.Context, matchID string, playerIDs []string) error {
	var list []domain.MatchAttendee
	for _, pid := range playerIDs {
		pos := domain.PositionDefender
		if pid == "gk1" {
			pos = domain.PositionKeeper
		}
		list = append(list, domain.MatchAttendee{
			MatchID:     matchID,
			PlayerID:    pid,
			Player:      domain.Player{ID: pid, DisplayName: "Player " + pid, PrimaryPosition: pos},
			CheckedInAt: time.Now(),
		})
	}
	m.attendees[matchID] = list
	return nil
}

func (m *mockMatchStoreForService) GetAttendees(ctx context.Context, matchID string) ([]domain.MatchAttendee, error) {
	return m.attendees[matchID], nil
}

func (m *mockMatchStoreForService) AddLateAttendee(ctx context.Context, matchID string, playerID string) error {
	m.attendees[matchID] = append(m.attendees[matchID], domain.MatchAttendee{
		MatchID:       matchID,
		PlayerID:      playerID,
		Player:        domain.Player{ID: playerID, DisplayName: "Late Arrival", PrimaryPosition: domain.PositionStriker},
		IsLateArrival: true,
	})
	return nil
}

func (m *mockMatchStoreForService) RemoveAttendee(ctx context.Context, matchID string, playerID string) error {
	var remaining []domain.MatchAttendee
	for _, a := range m.attendees[matchID] {
		if a.PlayerID != playerID {
			remaining = append(remaining, a)
		}
	}
	m.attendees[matchID] = remaining
	return nil
}

func TestMatchService_SummaryCalculations(t *testing.T) {
	store := &mockMatchStoreForService{
		matches:   make(map[string]domain.Match),
		attendees: make(map[string][]domain.MatchAttendee),
	}
	store.matches["m-7v7"] = domain.Match{
		ID:     "m-7v7",
		Title:  "Saturday 7v7",
		Format: domain.FormatSevenASide,
	}

	svc := NewMatchService(store)

	// Sync 15 players (1 GK, 14 Defenders)
	playerIDs := []string{"gk1"}
	for i := 2; i <= 15; i++ {
		playerIDs = append(playerIDs, "p"+string(rune(i+'0')))
	}

	summary, err := svc.SyncAttendees(context.Background(), "m-7v7", playerIDs)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if summary.TotalAttendees != 15 {
		t.Errorf("expected 15 attendees, got %d", summary.TotalAttendees)
	}
	// For 15 attendees at 7-a-side:
	// Suggested teams = 15 / 7 = 2 full teams (14 players)
	// Substitutes = 15 % 7 = 1 substitute
	if summary.SuggestedTeams != 2 {
		t.Errorf("expected 2 suggested teams, got %d", summary.SuggestedTeams)
	}
	if summary.SubstitutesCount != 1 {
		t.Errorf("expected 1 substitute, got %d", summary.SubstitutesCount)
	}
	// 2 teams need 2 Keepers, but only 1 GK attended:
	// Keeper deficit = 2 - 1 = 1
	if summary.KeeperDeficit != 1 {
		t.Errorf("expected keeper deficit of 1, got %d", summary.KeeperDeficit)
	}
}
