package handler

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/matchmaker"
)

type mockTeamService struct {
	generateFn func(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error)
	getFn      func(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error)
}

func (m *mockTeamService) GenerateTeams(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error) {
	if m.generateFn != nil {
		return m.generateFn(ctx, matchID, subPolicyOverride)
	}
	return nil, domain.ErrMatchNotFound
}

func (m *mockTeamService) GetTeams(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
	if m.getFn != nil {
		return m.getFn(ctx, matchID)
	}
	return nil, domain.ErrMatchNotFound
}

func TestTeamHandler_GenerateTeams_Success(t *testing.T) {
	svc := &mockTeamService{
		generateFn: func(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error) {
			return &domain.MatchTeamsResponse{
				MatchID:   matchID,
				Format:    domain.FormatSevenASide,
				TeamCount: 2,
			}, nil
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/matches/{id}/generate-teams", h.GenerateTeams)

	body, _ := json.Marshal(map[string]string{"sub_policy": "STRICT_SUBSTITUTES"})
	req := httptest.NewRequest("POST", "/api/v1/matches/match-123/generate-teams", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d: %s", w.Code, w.Body.String())
	}

	var res map[string]interface{}
	if err := json.Unmarshal(w.Body.Bytes(), &res); err != nil {
		t.Fatalf("failed decoding response: %v", err)
	}
	data, ok := res["data"].(map[string]interface{})
	if !ok || data["match_id"] != "match-123" {
		t.Errorf("expected match_id match-123, got %v", data)
	}
}

func TestTeamHandler_GenerateTeams_NotFound(t *testing.T) {
	svc := &mockTeamService{
		generateFn: func(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error) {
			return nil, domain.ErrMatchNotFound
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/matches/{id}/generate-teams", h.GenerateTeams)

	req := httptest.NewRequest("POST", "/api/v1/matches/unknown-match/generate-teams", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusNotFound {
		t.Fatalf("expected status 404, got %d: %s", w.Code, w.Body.String())
	}
}

func TestTeamHandler_GenerateTeams_InsufficientAttendees(t *testing.T) {
	svc := &mockTeamService{
		generateFn: func(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error) {
			return nil, matchmaker.ErrInsufficientAttendees
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/matches/{id}/generate-teams", h.GenerateTeams)

	req := httptest.NewRequest("POST", "/api/v1/matches/match-short/generate-teams", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusUnprocessableEntity {
		t.Fatalf("expected status 422, got %d: %s", w.Code, w.Body.String())
	}
}

func TestTeamHandler_GetTeams_Success(t *testing.T) {
	svc := &mockTeamService{
		getFn: func(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
			return &domain.MatchTeamsResponse{
				MatchID:   matchID,
				Format:    domain.FormatSevenASide,
				TeamCount: 2,
			}, nil
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Get("/api/v1/matches/{id}/teams", h.GetTeams)

	req := httptest.NewRequest("GET", "/api/v1/matches/match-123/teams", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d: %s", w.Code, w.Body.String())
	}
}
