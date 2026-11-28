package handler

import (
	"crypto/subtle"
	"encoding/json"
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/williskipsjr/f4f-arena/backend/internal/auth"
	"github.com/williskipsjr/f4f-arena/backend/internal/repository/postgres"
)

type LoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type RegisterRequest struct {
	DisplayName     string  `json:"display_name"`
	Username        string  `json:"username"`
	Phone           string  `json:"phone"`
	Age             *int    `json:"age,omitempty"`
	Password        string  `json:"password"`
	FirebaseIDToken string  `json:"firebase_id_token,omitempty"`
}

type ResetPinRequest struct {
	Username    string `json:"username"`
	Phone       string `json:"phone"`
	NewPassword string `json:"new_password"`
}

type ChangePasswordRequest struct {
	CurrentPassword string `json:"current_password"`
	NewPassword     string `json:"new_password"`
}

type ClaimProfileRequest struct {
	PlayerID string `json:"player_id"`
	Phone    string `json:"phone"`
	Password string `json:"password"`
}

type PhoneLookupRequest struct {
	Phone string `json:"phone"`
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

	info, err := h.accountRepo.RegisterPlayer(r.Context(), req.DisplayName, req.Username, req.Phone, req.Password, req.Age)
	if err != nil {
		if errors.Is(err, postgres.ErrUsernameTaken) {
			writeError(w, http.StatusConflict, "USERNAME_TAKEN", "Phone number or username is already registered")
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

	// 1. Check Admin credentials first (DB-backed hash overrides env fallback once set)
	if strings.EqualFold(strings.TrimSpace(req.Username), h.adminUsername) {
		var (
			adminPlayerID string
			valid         bool
		)
		if h.accountRepo != nil {
			adminPlayerID, valid, _ = h.accountRepo.VerifyAdminPassword(r.Context(), h.adminUsername, req.Password, h.adminPassword)
		} else if h.adminPassword != "" && subtle.ConstantTimeCompare([]byte(req.Password), []byte(h.adminPassword)) == 1 {
			valid = true
		}
		if valid {
			token, expiresAt, err := h.tokenService.GenerateTokenWithRole(h.adminUsername, "ADMIN", adminPlayerID)
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

func (h *AuthHandler) ResetPin(w http.ResponseWriter, r *http.Request) {
	if h.accountRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Database unavailable")
		return
	}

	var req ResetPinRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	if err := h.accountRepo.ResetPinByPhone(r.Context(), req.Username, req.Phone, req.NewPassword); err != nil {
		if errors.Is(err, postgres.ErrInvalidCredentials) {
			writeError(w, http.StatusNotFound, "NOT_FOUND", "Account not found")
			return
		}
		writeError(w, http.StatusBadRequest, "RESET_FAILED", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{
		"status":  "ok",
		"message": "PIN updated successfully",
	})
}

func (h *AuthHandler) ChangePassword(w http.ResponseWriter, r *http.Request) {
	if h.accountRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Database unavailable")
		return
	}
	claims, _ := r.Context().Value(auth.AdminContextKey).(*auth.Claims)
	if claims == nil || claims.Username == "" {
		writeError(w, http.StatusUnauthorized, "UNAUTHORIZED", "Authentication required")
		return
	}

	var req ChangePasswordRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}

	if err := h.accountRepo.ChangePassword(
		r.Context(),
		claims.Username,
		claims.Role,
		req.CurrentPassword,
		req.NewPassword,
		h.adminPassword,
	); err != nil {
		if errors.Is(err, postgres.ErrInvalidCredentials) {
			writeError(w, http.StatusUnauthorized, "INVALID_PASSWORD", "Current password is incorrect")
			return
		}
		writeError(w, http.StatusBadRequest, "CHANGE_FAILED", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{
		"status":  "ok",
		"message": "Password updated successfully",
	})
}

func (h *AuthHandler) ClaimProfile(w http.ResponseWriter, r *http.Request) {
	if h.accountRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Database unavailable")
		return
	}
	var req ClaimProfileRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}
	info, err := h.accountRepo.ClaimProfile(r.Context(), req.PlayerID, req.Phone, req.Password)
	if err != nil {
		writeError(w, http.StatusBadRequest, "CLAIM_FAILED", err.Error())
		return
	}
	token, expiresAt, err := h.tokenService.GenerateTokenWithRole(info.Username, info.Role, info.PlayerID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to generate token")
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

func (h *AuthHandler) PhoneLookup(w http.ResponseWriter, r *http.Request) {
	if h.accountRepo == nil {
		writeError(w, http.StatusServiceUnavailable, "DB_UNAVAILABLE", "Database unavailable")
		return
	}
	var req PhoneLookupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "BAD_REQUEST", "Invalid JSON request body")
		return
	}
	res, err := h.accountRepo.LookupByPhone(r.Context(), req.Phone)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "LOOKUP_FAILED", err.Error())
		return
	}
	writeJSON(w, http.StatusOK, res)
}
