package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"time"

	"github.com/williskipsjr/f4f-arena/backend/internal/auth"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/repository/postgres"
)

type LoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type RegisterRequest struct {
	DisplayName     string           `json:"display_name"`
	Username        string           `json:"username"`
	Phone           string           `json:"phone,omitempty"`
	Password        string           `json:"password"`
	PrimaryPosition *domain.Position `json:"primary_position,omitempty"`
	PlayerID        *string          `json:"player_id,omitempty"`
}

type LoginResponse struct {
	Token       string    `json:"token"`
	TokenType   string    `json:"token_type"`
	ExpiresAt   time.Time `json:"expires_at"`
	Role        string    `json:"role"`
	PlayerID    string    `json:"player_id,omitempty"`
	DisplayName string    `json:"display_name,omitempty"`
}

type AuthHandler struct {
	adminUsername string
	adminPassword string
	tokenService  *auth.TokenService
	accountRepo   *postgres.AccountRepository
}

func NewAuthHandler(adminUsername, adminPassword string, tokenService *auth.TokenService) *AuthHandler {
	return &AuthHandler{
		adminUsername: adminUsername,
		adminPassword: adminPassword,
		tokenService:  tokenService,
	}
}

func (h *AuthHandler) WithAccountRepo(repo *postgres.AccountRepository) *AuthHandler {
	h.accountRepo = repo
	return h
}

func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	if h.accountRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Database unavailable for registration")
		return
	}

	var req RegisterRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	pos := domain.PositionMidfielder
	if req.PrimaryPosition != nil && req.PrimaryPosition.IsValid() {
		pos = *req.PrimaryPosition
	}

	info, err := h.accountRepo.RegisterPlayer(r.Context(), req.DisplayName, req.Username, req.Phone, req.Password, pos, req.PlayerID)
	if err != nil {
		if errors.Is(err, postgres.ErrUsernameTaken) {
			writeError(w, http.StatusConflict, "USERNAME_TAKEN", "Username is already registered")
			return
		}
		writeError(w, http.StatusBadRequest, "VALIDATION_FAILED", err.Error())
		return
	}

	token, expiresAt, err := h.tokenService.GenerateTokenWithRole(info.Username, info.Role, info.PlayerID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to generate authorization token")
		return
	}

	writeJSON(w, http.StatusCreated, LoginResponse{
		Token:       token,
		TokenType:   "Bearer",
		ExpiresAt:   expiresAt.UTC(),
		Role:        info.Role,
		PlayerID:    info.PlayerID,
		DisplayName: info.DisplayName,
	})
}

func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"error":{"code":"METHOD_NOT_ALLOWED","message":"Method not allowed"}}`, http.StatusMethodNotAllowed)
		return
	}

	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	// 1. Check Admin credentials first
	if req.Username == h.adminUsername && req.Password == h.adminPassword {
		adminPlayerID := ""
		if h.accountRepo != nil {
			if pid, err := h.accountRepo.EnsureAdminPlayer(r.Context(), req.Username); err == nil {
				adminPlayerID = pid
			}
		}
		token, expiresAt, err := h.tokenService.GenerateTokenWithRole(req.Username, "ADMIN", adminPlayerID)
		if err != nil {
			writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to generate authorization token")
			return
		}
		writeJSON(w, http.StatusOK, LoginResponse{
			Token:       token,
			TokenType:   "Bearer",
			ExpiresAt:   expiresAt.UTC(),
			Role:        "ADMIN",
			PlayerID:    adminPlayerID,
			DisplayName: "Admin",
		})
		return
	}

	// 2. Fall back to registered community player accounts
	if h.accountRepo != nil {
		info, err := h.accountRepo.Authenticate(r.Context(), req.Username, req.Password)
		if err == nil {
			token, expiresAt, err := h.tokenService.GenerateTokenWithRole(info.Username, info.Role, info.PlayerID)
			if err != nil {
				writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to generate authorization token")
				return
			}
			writeJSON(w, http.StatusOK, LoginResponse{
				Token:       token,
				TokenType:   "Bearer",
				ExpiresAt:   expiresAt.UTC(),
				Role:        info.Role,
				PlayerID:    info.PlayerID,
				DisplayName: info.DisplayName,
			})
			return
		}
	}

	writeError(w, http.StatusUnauthorized, "UNAUTHORIZED", "Invalid username or password")
}
