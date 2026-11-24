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

type mockPlayerStore struct {
	players []domain.Player
}

func (m *mockPlayerStore) List(ctx context.Context, filter domain.PlayerFilter) ([]domain.Player, error) {
	var result []domain.Player
	for _, p := range m.players {
		if filter.IsActive != nil && p.IsActive != *filter.IsActive {
			continue
		}
		if filter.Position != nil && p.PrimaryPosition != *filter.Position {
			continue
		}
		result = append(result, p)
	}
	return result, nil
}

func (m *mockPlayerStore) GetByID(ctx context.Context, id string) (*domain.Player, error) {
	for _, p := range m.players {
		if p.ID == id {
			return &p, nil
		}
	}
	return nil, nil
}

func (m *mockPlayerStore) Create(ctx context.Context, p *domain.Player) error {
	p.ID = "generated-uuid-1"
	p.CreatedAt = time.Now()
	p.UpdatedAt = time.Now()
	m.players = append(m.players, *p)
	return nil
}

func (m *mockPlayerStore) Update(ctx context.Context, p *domain.Player) error {
	for i, existing := range m.players {
		if existing.ID == p.ID {
			m.players[i] = *p
			return nil
		}
	}
	return nil
}

func (m *mockPlayerStore) FindSimilarNames(ctx context.Context, name string, excludeID string) ([]string, error) {
	return nil, nil
}

func TestPlayerHandler_List(t *testing.T) {
	mockStore := &mockPlayerStore{
		players: []domain.Player{
			{ID: "1", DisplayName: "Lenpu", PrimaryPosition: domain.PositionKeeper, IsActive: true},
			{ID: "2", DisplayName: "Robert", PrimaryPosition: domain.PositionDefender, IsActive: true},
		},
	}
	svc := service.NewPlayerService(mockStore)
	h := NewPlayerHandler(svc)

	r := chi.NewRouter()
	r.Get("/api/v1/players", h.List)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/players?position=KEEPER", nil)
	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected 200 OK, got %d", rr.Code)
	}

	var resp struct {
		Data  []domain.Player `json:"data"`
		Total int             `json:"total"`
	}
	if err := json.NewDecoder(rr.Body).Decode(&resp); err != nil {
		t.Fatalf("failed decoding response: %v", err)
	}

	if resp.Total != 1 {
		t.Errorf("expected 1 keeper, got %d", resp.Total)
	}
	if resp.Data[0].DisplayName != "Lenpu" {
		t.Errorf("expected Lenpu, got %s", resp.Data[0].DisplayName)
	}
}

func TestPlayerHandler_Create(t *testing.T) {
	mockStore := &mockPlayerStore{}
	svc := service.NewPlayerService(mockStore)
	h := NewPlayerHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/players", h.Create)

	payload := domain.CreatePlayerInput{
		DisplayName:     "James Singson",
		PrimaryPosition: domain.PositionKeeper,
	}
	body, _ := json.Marshal(payload)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/players", bytes.NewReader(body))
	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusCreated {
		t.Fatalf("expected 201 Created, got %d", rr.Code)
	}

	var resp struct {
		Data domain.Player `json:"data"`
	}
	if err := json.NewDecoder(rr.Body).Decode(&resp); err != nil {
		t.Fatalf("failed decoding response: %v", err)
	}

	if resp.Data.DisplayName != "James Singson" {
		t.Errorf("expected 'James Singson', got %s", resp.Data.DisplayName)
	}
}
