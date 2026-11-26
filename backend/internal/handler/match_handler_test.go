package handler

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/service"
)

type mockMatchStore struct {
	matches   map[string]domain.Match
	attendees map[string][]domain.MatchAttendee
}

func newMockMatchStore() *mockMatchStore {
	return &mockMatchStore{
		matches:   make(map[string]domain.Match),
		attendees: make(map[string][]domain.MatchAttendee),
	}
}

func (m *mockMatchStore) Create(ctx context.Context, match *domain.Match) error {
	match.ID = "match-123"
	match.CreatedAt = time.Now()
	match.UpdatedAt = time.Now()
	m.matches[match.ID] = *match
	return nil
}

func (m *mockMatchStore) GetByID(ctx context.Context, id string) (*domain.Match, error) {
	match, exists := m.matches[id]
	if !exists {
		return nil, domain.ErrMatchNotFound
	}
	return &match, nil
}

func (m *mockMatchStore) List(ctx context.Context, status *domain.MatchStatus) ([]domain.Match, error) {
	var list []domain.Match
	for _, match := range m.matches {
		list = append(list, match)
	}
	return list, nil
}

func (m *mockMatchStore) Update(ctx context.Context, match *domain.Match) error {
	m.matches[match.ID] = *match
	return nil
}

func (m *mockMatchStore) SyncAttendees(ctx context.Context, matchID string, playerIDs []string) error {
	var att []domain.MatchAttendee
	for _, pid := range playerIDs {
		att = append(att, domain.MatchAttendee{
			MatchID:     matchID,
			PlayerID:    pid,
			Player:      domain.Player{ID: pid, DisplayName: "Player " + pid, PrimaryPosition: domain.PositionDefender},
			CheckedInAt: time.Now(),
		})
	}
	m.attendees[matchID] = att
	return nil
}

func (m *mockMatchStore) GetAttendees(ctx context.Context, matchID string) ([]domain.MatchAttendee, error) {
	return m.attendees[matchID], nil
}

func (m *mockMatchStore) AddLateAttendee(ctx context.Context, matchID string, playerID string) error {
	m.attendees[matchID] = append(m.attendees[matchID], domain.MatchAttendee{
		MatchID:       matchID,
		PlayerID:      playerID,
		Player:        domain.Player{ID: playerID, DisplayName: "Late Player", PrimaryPosition: domain.PositionMidfielder},
		IsLateArrival: true,
		CheckedInAt:   time.Now(),
	})
	return nil
}

func (m *mockMatchStore) RemoveAttendee(ctx context.Context, matchID string, playerID string) error {
	var remaining []domain.MatchAttendee
	for _, a := range m.attendees[matchID] {
		if a.PlayerID != playerID {
			remaining = append(remaining, a)
		}
	}
	m.attendees[matchID] = remaining
	return nil
}

func (m *mockMatchStore) Delete(ctx context.Context, id string) error {
	delete(m.matches, id)
	return nil
}

func TestMatchHandler_Create(t *testing.T) {
	store := newMockMatchStore()
	svc := service.NewMatchService(store)
	h := NewMatchHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/matches", h.Create)

	payload := domain.CreateMatchInput{
		Title:  "Saturday Turf 7v7",
		Format: domain.FormatSevenASide,
	}
	body, _ := json.Marshal(payload)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/matches", bytes.NewReader(body))
	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusCreated {
		t.Fatalf("expected 201 Created, got %d", rr.Code)
	}

	var resp struct {
		Data domain.Match `json:"data"`
	}
	if err := json.NewDecoder(rr.Body).Decode(&resp); err != nil {
		t.Fatalf("failed decoding: %v", err)
	}

	if resp.Data.Title != "Saturday Turf 7v7" {
		t.Errorf("expected title 'Saturday Turf 7v7', got %s", resp.Data.Title)
	}
	if resp.Data.Format != domain.FormatSevenASide {
		t.Errorf("expected format SEVEN_A_SIDE, got %s", resp.Data.Format)
	}
}

func TestMatchHandler_SyncAttendees(t *testing.T) {
	store := newMockMatchStore()
	store.matches["match-123"] = domain.Match{
		ID:     "match-123",
		Title:  "Session",
		Format: domain.FormatSevenASide,
		Status: domain.StatusDraft,
	}
	svc := service.NewMatchService(store)
	h := NewMatchHandler(svc)

	r := chi.NewRouter()
	r.Put("/api/v1/matches/{id}/attendees", h.SyncAttendees)

	payload := domain.SyncAttendeesInput{
		PlayerIDs: []string{"p1", "p2", "p3"},
	}
	body, _ := json.Marshal(payload)

	req := httptest.NewRequest(http.MethodPut, "/api/v1/matches/match-123/attendees", bytes.NewReader(body))
	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected 200 OK, got %d", rr.Code)
	}

	var resp struct {
		Data domain.AttendanceSummary `json:"data"`
	}
	if err := json.NewDecoder(rr.Body).Decode(&resp); err != nil {
		t.Fatalf("failed decoding summary: %v", err)
	}

	if resp.Data.TotalAttendees != 3 {
		t.Errorf("expected 3 attendees, got %d", resp.Data.TotalAttendees)
	}
}
