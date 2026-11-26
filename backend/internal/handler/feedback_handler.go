package handler

import (
	"encoding/json"
	"net/http"

	"github.com/williskipsjr/f4f-arena/backend/internal/auth"
	"github.com/williskipsjr/f4f-arena/backend/internal/repository/postgres"
)

type CreateFeedbackRequest struct {
	Category string `json:"category"`
	Message  string `json:"message"`
}

type FeedbackHandler struct {
	repo *postgres.FeedbackRepository
}

func NewFeedbackHandler(repo *postgres.FeedbackRepository) *FeedbackHandler {
	return &FeedbackHandler{repo: repo}
}

func (h *FeedbackHandler) Create(w http.ResponseWriter, r *http.Request) {
	if h.repo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Database unavailable")
		return
	}

	var req CreateFeedbackRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	claims, _ := r.Context().Value(auth.AdminContextKey).(*auth.Claims)
	var playerID *string
	author := "Community Member"
	if claims != nil {
		if claims.PlayerID != "" {
			pid := claims.PlayerID
			playerID = &pid
		}
		if claims.Username != "" {
			author = claims.Username
		}
	}

	item, err := h.repo.Create(r.Context(), playerID, author, req.Category, req.Message)
	if err != nil {
		writeError(w, http.StatusBadRequest, "VALIDATION_FAILED", err.Error())
		return
	}

	writeJSON(w, http.StatusCreated, map[string]interface{}{
		"data": item,
	})
}

func (h *FeedbackHandler) List(w http.ResponseWriter, r *http.Request) {
	if h.repo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Database unavailable")
		return
	}

	items, err := h.repo.List(r.Context(), 30)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": items,
	})
}
