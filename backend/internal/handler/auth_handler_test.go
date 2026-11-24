package handler

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/williskipsjr/f4f-arena/backend/internal/auth"
)

func TestAuthHandler_LoginSuccess(t *testing.T) {
	tokenSvc := auth.NewTokenService("test-secret-key-32-bytes-long!!", 1*time.Hour)
	handler := NewAuthHandler("admin", "pass123", tokenSvc)

	body, _ := json.Marshal(LoginRequest{
		Username: "admin",
		Password: "pass123",
	})

	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", bytes.NewReader(body))
	rr := httptest.NewRecorder()

	handler.Login(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rr.Code)
	}

	var resp LoginResponse
	if err := json.NewDecoder(rr.Body).Decode(&resp); err != nil {
		t.Fatalf("failed to decode login response: %v", err)
	}

	if resp.Token == "" {
		t.Error("expected non-empty token")
	}
	if resp.TokenType != "Bearer" {
		t.Errorf("expected token type 'Bearer', got '%s'", resp.TokenType)
	}
}

func TestAuthHandler_LoginInvalidCredentials(t *testing.T) {
	tokenSvc := auth.NewTokenService("test-secret-key-32-bytes-long!!", 1*time.Hour)
	handler := NewAuthHandler("admin", "pass123", tokenSvc)

	body, _ := json.Marshal(LoginRequest{
		Username: "admin",
		Password: "wrongpassword",
	})

	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", bytes.NewReader(body))
	rr := httptest.NewRecorder()

	handler.Login(rr, req)

	if rr.Code != http.StatusUnauthorized {
		t.Fatalf("expected status 401, got %d", rr.Code)
	}
}
