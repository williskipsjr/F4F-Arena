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
	"github.com/williskipsjr/f4f-arena/backend/internal/service"
)

type mockTeamService struct {
	generateFn       func(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error)
	getFn            func(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error)
	swapFn           func(ctx context.Context, matchID string, input domain.SwapPlayersInput) (*domain.MatchTeamsResponse, error)
	moveFn           func(ctx context.Context, matchID string, input domain.MovePlayerInput) (*domain.MatchTeamsResponse, error)
	finaliseFn       func(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error)
	whatsappExportFn func(ctx context.Context, matchID string) (*domain.WhatsAppExportResponse, error)
}

func (m *mockTeamService) GenerateTeams(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error) {
	if m.generateFn != nil {
		return m.generateFn(ctx, matchID, subPolicyOverride)
	}
	return nil, domain.ErrMatchNotFound
}

func (m *mockTeamService) GenerateTeamsWithOptions(ctx context.Context, matchID string, opts service.GenerateTeamsOptions) (*domain.MatchTeamsResponse, error) {
	return m.GenerateTeams(ctx, matchID, opts.SubPolicy)
}

func (m *mockTeamService) GetTeams(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
	if m.getFn != nil {
		return m.getFn(ctx, matchID)
	}
	return nil, domain.ErrMatchNotFound
}

func (m *mockTeamService) SwapPlayers(ctx context.Context, matchID string, input domain.SwapPlayersInput) (*domain.MatchTeamsResponse, error) {
	if m.swapFn != nil {
		return m.swapFn(ctx, matchID, input)
	}
	return nil, domain.ErrMatchNotFound
}

func (m *mockTeamService) MovePlayer(ctx context.Context, matchID string, input domain.MovePlayerInput) (*domain.MatchTeamsResponse, error) {
	if m.moveFn != nil {
		return m.moveFn(ctx, matchID, input)
	}
	return nil, domain.ErrMatchNotFound
}

func (m *mockTeamService) FinaliseMatch(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
	if m.finaliseFn != nil {
		return m.finaliseFn(ctx, matchID)
	}
	return nil, domain.ErrMatchNotFound
}

func (m *mockTeamService) GetWhatsAppExport(ctx context.Context, matchID string) (*domain.WhatsAppExportResponse, error) {
	if m.whatsappExportFn != nil {
		return m.whatsappExportFn(ctx, matchID)
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

func TestTeamHandler_SwapPlayers_Success(t *testing.T) {
	svc := &mockTeamService{
		swapFn: func(ctx context.Context, matchID string, input domain.SwapPlayersInput) (*domain.MatchTeamsResponse, error) {
			return &domain.MatchTeamsResponse{
				MatchID:   matchID,
				TeamCount: 2,
			}, nil
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/matches/{id}/assignments/swap", h.SwapPlayers)

	body, _ := json.Marshal(domain.SwapPlayersInput{
		PlayerAID: "p-1",
		PlayerBID: "p-2",
	})
	req := httptest.NewRequest("POST", "/api/v1/matches/match-123/assignments/swap", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d: %s", w.Code, w.Body.String())
	}
}

func TestTeamHandler_SwapPlayers_CannotSwapSelf(t *testing.T) {
	svc := &mockTeamService{
		swapFn: func(ctx context.Context, matchID string, input domain.SwapPlayersInput) (*domain.MatchTeamsResponse, error) {
			return nil, domain.ErrCannotSwapSelf
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/matches/{id}/assignments/swap", h.SwapPlayers)

	body, _ := json.Marshal(domain.SwapPlayersInput{
		PlayerAID: "p-1",
		PlayerBID: "p-1",
	})
	req := httptest.NewRequest("POST", "/api/v1/matches/match-123/assignments/swap", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected status 400, got %d: %s", w.Code, w.Body.String())
	}
}

func TestTeamHandler_MovePlayer_Success(t *testing.T) {
	svc := &mockTeamService{
		moveFn: func(ctx context.Context, matchID string, input domain.MovePlayerInput) (*domain.MatchTeamsResponse, error) {
			return &domain.MatchTeamsResponse{
				MatchID:   matchID,
				TeamCount: 2,
			}, nil
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/matches/{id}/assignments/move", h.MovePlayer)

	targetTeam := "team-1"
	body, _ := json.Marshal(domain.MovePlayerInput{
		PlayerID:     "p-1",
		TargetTeamID: &targetTeam,
		IsSubstitute: false,
	})
	req := httptest.NewRequest("POST", "/api/v1/matches/match-123/assignments/move", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d: %s", w.Code, w.Body.String())
	}
}

func TestTeamHandler_FinaliseMatch_Success(t *testing.T) {
	svc := &mockTeamService{
		finaliseFn: func(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
			return &domain.MatchTeamsResponse{
				MatchID: matchID,
				Status:  domain.StatusFinalised,
			}, nil
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Post("/api/v1/matches/{id}/finalise", h.FinaliseMatch)

	req := httptest.NewRequest("POST", "/api/v1/matches/match-123/finalise", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d: %s", w.Code, w.Body.String())
	}
}

func TestTeamHandler_GetWhatsAppExport_Success(t *testing.T) {
	svc := &mockTeamService{
		whatsappExportFn: func(ctx context.Context, matchID string) (*domain.WhatsAppExportResponse, error) {
			return &domain.WhatsAppExportResponse{
				MatchID:       matchID,
				FormattedText: "⚽ *F4F ARENA*",
			}, nil
		},
	}
	h := NewTeamHandler(svc)

	r := chi.NewRouter()
	r.Get("/api/v1/matches/{id}/whatsapp-export", h.GetWhatsAppExport)

	req := httptest.NewRequest("GET", "/api/v1/matches/match-123/whatsapp-export", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d: %s", w.Code, w.Body.String())
	}
}
