package handler

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/williskipsjr/f4f-arena/backend/internal/auth"
)

type LoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type LoginResponse struct {
	Token     string    `json:"token"`
	TokenType string    `json:"token_type"`
	ExpiresAt time.Time `json:"expires_at"`
}

type AuthHandler struct {
	adminUsername string
	adminPassword string
	tokenService  *auth.TokenService
}

func NewAuthHandler(adminUsername, adminPassword string, tokenService *auth.TokenService) *AuthHandler {
	return &AuthHandler{
		adminUsername: adminUsername,
		adminPassword: adminPassword,
		tokenService:  tokenService,
	}
}

func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"error":{"code":"METHOD_NOT_ALLOWED","message":"Method not allowed"}}`, http.StatusMethodNotAllowed)
		return
	}

	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_ = json.NewEncoder(w).Encode(map[string]interface{}{
			"error": map[string]string{
				"code":    "BAD_REQUEST",
				"message": "Invalid JSON request body",
			},
		})
		return
	}

	// Simple constant-time comparison or direct credential validation
	if req.Username != h.adminUsername || req.Password != h.adminPassword {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusUnauthorized)
		_ = json.NewEncoder(w).Encode(map[string]interface{}{
			"error": map[string]string{
				"code":    "UNAUTHORIZED",
				"message": "Invalid admin username or password",
			},
		})
		return
	}

	token, expiresAt, err := h.tokenService.GenerateToken(req.Username)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		_ = json.NewEncoder(w).Encode(map[string]interface{}{
			"error": map[string]string{
				"code":    "INTERNAL_ERROR",
				"message": "Failed to generate authorization token",
			},
		})
		return
	}

	resp := LoginResponse{
		Token:     token,
		TokenType: "Bearer",
		ExpiresAt: expiresAt.UTC(),
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(resp)
}
