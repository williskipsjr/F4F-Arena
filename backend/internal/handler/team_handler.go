package handler

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/matchmaker"
	"github.com/williskipsjr/f4f-arena/backend/internal/service"
)

type TeamHandler struct {
	teamService service.TeamService
}

func NewTeamHandler(teamService service.TeamService) *TeamHandler {
	return &TeamHandler{teamService: teamService}
}

type GenerateTeamsRequest struct {
	SubPolicy *domain.SubstitutePolicy `json:"sub_policy,omitempty"`
}

func (h *TeamHandler) GenerateTeams(w http.ResponseWriter, r *http.Request) {
	matchID := chi.URLParam(r, "id")
	if matchID == "" {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Match ID is required")
		return
	}

	var req GenerateTeamsRequest
	if r.Body != nil && r.ContentLength > 0 {
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
			return
		}
	}

	resp, err := h.teamService.GenerateTeams(r.Context(), matchID, req.SubPolicy)
	if err != nil {
		if errors.Is(err, domain.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		if errors.Is(err, matchmaker.ErrNoAttendees) || errors.Is(err, matchmaker.ErrInsufficientAttendees) {
			writeError(w, http.StatusUnprocessableEntity, "INSUFFICIENT_ATTENDEES", err.Error())
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": resp,
	})
}

func (h *TeamHandler) GetTeams(w http.ResponseWriter, r *http.Request) {
	matchID := chi.URLParam(r, "id")
	if matchID == "" {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Match ID is required")
		return
	}

	resp, err := h.teamService.GetTeams(r.Context(), matchID)
	if err != nil {
		if errors.Is(err, domain.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": resp,
	})
}

func (h *TeamHandler) SwapPlayers(w http.ResponseWriter, r *http.Request) {
	matchID := chi.URLParam(r, "id")
	if matchID == "" {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Match ID is required")
		return
	}

	var input domain.SwapPlayersInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	resp, err := h.teamService.SwapPlayers(r.Context(), matchID, input)
	if err != nil {
		if errors.Is(err, domain.ErrMatchNotFound) || errors.Is(err, domain.ErrAssignmentNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", err.Error())
			return
		}
		if errors.Is(err, domain.ErrMatchCompleted) {
			writeError(w, http.StatusConflict, "MATCH_LOCKED", err.Error())
			return
		}
		if errors.Is(err, domain.ErrCannotSwapSelf) {
			writeError(w, http.StatusBadRequest, "BAD_REQUEST", err.Error())
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": resp,
	})
}

func (h *TeamHandler) MovePlayer(w http.ResponseWriter, r *http.Request) {
	matchID := chi.URLParam(r, "id")
	if matchID == "" {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Match ID is required")
		return
	}

	var input domain.MovePlayerInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	resp, err := h.teamService.MovePlayer(r.Context(), matchID, input)
	if err != nil {
		if errors.Is(err, domain.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		if errors.Is(err, domain.ErrTargetTeamNotFound) || errors.Is(err, domain.ErrPlayerNotAttendee) {
			writeError(w, http.StatusBadRequest, "BAD_REQUEST", err.Error())
			return
		}
		if errors.Is(err, domain.ErrMatchCompleted) {
			writeError(w, http.StatusConflict, "MATCH_LOCKED", err.Error())
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": resp,
	})
}

func (h *TeamHandler) FinaliseMatch(w http.ResponseWriter, r *http.Request) {
	matchID := chi.URLParam(r, "id")
	if matchID == "" {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Match ID is required")
		return
	}

	resp, err := h.teamService.FinaliseMatch(r.Context(), matchID)
	if err != nil {
		if errors.Is(err, domain.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		if errors.Is(err, domain.ErrMatchCompleted) {
			writeError(w, http.StatusConflict, "MATCH_LOCKED", err.Error())
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": resp,
	})
}

func (h *TeamHandler) GetWhatsAppExport(w http.ResponseWriter, r *http.Request) {
	matchID := chi.URLParam(r, "id")
	if matchID == "" {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Match ID is required")
		return
	}

	resp, err := h.teamService.GetWhatsAppExport(r.Context(), matchID)
	if err != nil {
		if errors.Is(err, domain.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": resp,
	})
}
