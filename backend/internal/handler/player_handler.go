package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/repository/postgres"
	"github.com/williskipsjr/f4f-arena/backend/internal/service"
)

type PlayerHandler struct {
	service *service.PlayerService
}

func NewPlayerHandler(service *service.PlayerService) *PlayerHandler {
	return &PlayerHandler{service: service}
}

func (h *PlayerHandler) List(w http.ResponseWriter, r *http.Request) {
	var filter domain.PlayerFilter

	if activeParam := r.URL.Query().Get("active"); activeParam != "" {
		if val, err := strconv.ParseBool(activeParam); err == nil {
			filter.IsActive = &val
		}
	}

	if posParam := r.URL.Query().Get("position"); posParam != "" {
		pos := domain.Position(strings.ToUpper(posParam))
		if pos.IsValid() {
			filter.Position = &pos
		}
	}

	if q := strings.TrimSpace(r.URL.Query().Get("query")); q != "" {
		filter.Query = &q
	}

	players, err := h.service.ListPlayers(r.Context(), filter)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to retrieve players: "+err.Error())
		return
	}

	if players == nil {
		players = []domain.Player{}
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data":  players,
		"total": len(players),
	})
}

func (h *PlayerHandler) Get(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	player, err := h.service.GetPlayer(r.Context(), id)
	if err != nil {
		if errors.Is(err, postgres.ErrPlayerNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Player not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to fetch player")
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": player,
	})
}

func (h *PlayerHandler) Create(w http.ResponseWriter, r *http.Request) {
	var input domain.CreatePlayerInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	player, warnings, err := h.service.CreatePlayer(r.Context(), input)
	if err != nil {
		writeError(w, http.StatusBadRequest, "VALIDATION_FAILED", err.Error())
		return
	}

	writeJSON(w, http.StatusCreated, map[string]interface{}{
		"data":     player,
		"warnings": warnings,
	})
}

func (h *PlayerHandler) Update(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	var input domain.UpdatePlayerInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	player, err := h.service.UpdatePlayer(r.Context(), id, input)
	if err != nil {
		if errors.Is(err, postgres.ErrPlayerNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Player not found")
			return
		}
		writeError(w, http.StatusBadRequest, "VALIDATION_FAILED", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": player,
	})
}

func writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(data)
}

func writeError(w http.ResponseWriter, status int, code, message string) {
	writeJSON(w, status, map[string]interface{}{
		"error": map[string]string{
			"code":    code,
			"message": message,
		},
	})
}
