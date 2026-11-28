package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/williskipsjr/f4f-arena/backend/internal/auth"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/repository/postgres"
	"github.com/williskipsjr/f4f-arena/backend/internal/service"
)

type MatchHandler struct {
	service      *service.MatchService
	voteRepo     *postgres.MatchRepository
	tokenService *auth.TokenService
}

func NewMatchHandler(service *service.MatchService) *MatchHandler {
	return &MatchHandler{service: service}
}

func (h *MatchHandler) WithVoting(repo *postgres.MatchRepository, tokenService *auth.TokenService) *MatchHandler {
	h.voteRepo = repo
	h.tokenService = tokenService
	return h
}

func (h *MatchHandler) Create(w http.ResponseWriter, r *http.Request) {
	var input domain.CreateMatchInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	match, err := h.service.CreateMatch(r.Context(), input)
	if err != nil {
		writeError(w, http.StatusBadRequest, "VALIDATION_FAILED", err.Error())
		return
	}

	writeJSON(w, http.StatusCreated, map[string]interface{}{
		"data": match,
	})
}

func (h *MatchHandler) List(w http.ResponseWriter, r *http.Request) {
	var statusFilter *domain.MatchStatus
	if s := strings.TrimSpace(r.URL.Query().Get("status")); s != "" {
		st := domain.MatchStatus(strings.ToUpper(s))
		if st.IsValid() {
			statusFilter = &st
		}
	}

	matches, err := h.service.ListMatches(r.Context(), statusFilter)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to list matches: "+err.Error())
		return
	}

	if matches == nil {
		matches = []domain.Match{}
	}

	// Optionally attach caller's vote if Authorization Bearer token belongs to a player
	if h.voteRepo != nil && h.tokenService != nil {
		if authHeader := r.Header.Get("Authorization"); strings.HasPrefix(authHeader, "Bearer ") {
			if claims, err := h.tokenService.ValidateToken(strings.TrimPrefix(authHeader, "Bearer ")); err == nil && claims.PlayerID != "" {
				if voteMap, err := h.voteRepo.GetPlayerVotesMap(r.Context(), claims.PlayerID); err == nil {
					for i := range matches {
						if v, ok := voteMap[matches[i].ID]; ok {
							val := v
							matches[i].MyVote = &val
						}
					}
				}
			}
		}
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data":  matches,
		"total": len(matches),
	})
}

func (h *MatchHandler) Get(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	match, err := h.service.GetMatch(r.Context(), id)
	if err != nil {
		if errors.Is(err, postgres.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to fetch match")
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": match,
	})
}

func (h *MatchHandler) Delete(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	if err := h.service.DeleteMatch(r.Context(), id); err != nil {
		if errors.Is(err, postgres.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to delete match: "+err.Error())
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"deleted": true,
	})
}

func (h *MatchHandler) Update(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	var input domain.UpdateMatchInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	match, err := h.service.UpdateMatch(r.Context(), id, input)
	if err != nil {
		if errors.Is(err, postgres.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		writeError(w, http.StatusBadRequest, "VALIDATION_FAILED", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": match,
	})
}

func (h *MatchHandler) SyncAttendees(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	var input domain.SyncAttendeesInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	summary, err := h.service.SyncAttendees(r.Context(), id, input.PlayerIDs)
	if err != nil {
		if errors.Is(err, postgres.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to sync attendees: "+err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": summary,
	})
}

func (h *MatchHandler) GetAttendees(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	summary, err := h.service.GetAttendanceSummary(r.Context(), id)
	if err != nil {
		if errors.Is(err, postgres.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to retrieve attendance summary: "+err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": summary,
	})
}

func (h *MatchHandler) AddLateArrival(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	var input domain.AddLateArrivalInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	summary, err := h.service.AddLateArrival(r.Context(), id, input.PlayerID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to add late arrival: "+err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": summary,
	})
}

func (h *MatchHandler) RemoveAttendee(w http.ResponseWriter, r *http.Request) {
	matchID := chi.URLParam(r, "id")
	playerID := chi.URLParam(r, "playerId")

	summary, err := h.service.RemoveAttendee(r.Context(), matchID, playerID)
	if err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": summary,
	})
}

func (h *MatchHandler) CastVote(w http.ResponseWriter, r *http.Request) {
	if h.voteRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Voting unavailable")
		return
	}

	matchID := chi.URLParam(r, "id")
	var input domain.CastVoteInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	vote := strings.ToUpper(strings.TrimSpace(input.Vote))
	if vote != "IN" && vote != "OUT" {
		writeError(w, http.StatusBadRequest, "VALIDATION_FAILED", "vote must be IN or OUT")
		return
	}

	claims, _ := r.Context().Value(auth.AdminContextKey).(*auth.Claims)
	playerID := ""
	if claims != nil && claims.PlayerID != "" {
		playerID = claims.PlayerID
	} else if claims != nil && claims.Role == "ADMIN" {
		if input.PlayerID != nil && *input.PlayerID != "" {
			playerID = *input.PlayerID
		} else if pid, err := h.voteRepo.EnsureAdminPlayerID(r.Context(), claims.Username); err == nil {
			playerID = pid
		}
	}
	if playerID == "" {
		writeError(w, http.StatusBadRequest, "PLAYER_REQUIRED", "Only player accounts (or admin specifying player_id) can vote")
		return
	}

	match, err := h.voteRepo.CastVote(r.Context(), matchID, playerID, vote)
	if err != nil {
		if errors.Is(err, postgres.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Match not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to record vote: "+err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": match,
	})
}

func (h *MatchHandler) GetVotes(w http.ResponseWriter, r *http.Request) {
	if h.voteRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Voting unavailable")
		return
	}

	matchID := chi.URLParam(r, "id")
	votes, err := h.voteRepo.GetMatchVotes(r.Context(), matchID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to fetch match votes: "+err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": votes,
	})
}

func (h *MatchHandler) GetComments(w http.ResponseWriter, r *http.Request) {
	if h.voteRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Comments unavailable")
		return
	}
	matchID := chi.URLParam(r, "id")
	comments, err := h.voteRepo.ListComments(r.Context(), matchID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to fetch comments: "+err.Error())
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"data": comments,
	})
}

func (h *MatchHandler) PostComment(w http.ResponseWriter, r *http.Request) {
	if h.voteRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Comments unavailable")
		return
	}
	claims, _ := r.Context().Value(auth.AdminContextKey).(*auth.Claims)
	if claims == nil {
		writeError(w, http.StatusUnauthorized, "UNAUTHORIZED", "Authentication required")
		return
	}

	matchID := chi.URLParam(r, "id")
	var input domain.CreateCommentInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	var playerIDPtr *string
	if claims.PlayerID != "" {
		pid := claims.PlayerID
		playerIDPtr = &pid
	}
	authorName := claims.Username
	if strings.EqualFold(claims.Role, "ADMIN") {
		authorName = "Admin"
	}

	comment, err := h.voteRepo.CreateComment(r.Context(), matchID, playerIDPtr, authorName, claims.Role, input.Message)
	if err != nil {
		writeError(w, http.StatusBadRequest, "VALIDATION_FAILED", err.Error())
		return
	}
	writeJSON(w, http.StatusCreated, map[string]interface{}{
		"data": comment,
	})
}

func (h *MatchHandler) DeleteComment(w http.ResponseWriter, r *http.Request) {
	if h.voteRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Comments unavailable")
		return
	}
	claims, ok := r.Context().Value(auth.AdminContextKey).(*auth.Claims)
	if !ok || claims == nil {
		writeError(w, http.StatusUnauthorized, "UNAUTHORIZED", "Authentication required")
		return
	}
	matchID := chi.URLParam(r, "id")
	commentID := chi.URLParam(r, "commentId")
	isAdmin := strings.EqualFold(claims.Role, "ADMIN")
	if err := h.voteRepo.DeleteComment(r.Context(), matchID, commentID, claims.PlayerID, isAdmin); err != nil {
		if errors.Is(err, postgres.ErrMatchNotFound) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Comment not found")
			return
		}
		if errors.Is(err, postgres.ErrCommentForbidden) {
			writeError(w, http.StatusForbidden, "FORBIDDEN", err.Error())
			return
		}
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to delete comment: "+err.Error())
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"deleted": true})
}
