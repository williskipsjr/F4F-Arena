package auth

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestTokenGenerationAndValidation(t *testing.T) {
	secret := "super-secure-dev-secret-key-32b"
	svc := NewTokenService(secret, 1*time.Hour)

	token, expiresAt, err := svc.GenerateToken("testadmin")
	if err != nil {
		t.Fatalf("failed to generate token: %v", err)
	}

	if token == "" {
		t.Fatal("expected non-empty token string")
	}

	if expiresAt.Before(time.Now()) {
		t.Fatal("expected expiry to be in the future")
	}

	claims, err := svc.ValidateToken(token)
	if err != nil {
		t.Fatalf("failed to validate token: %v", err)
	}

	if claims.Username != "testadmin" {
		t.Errorf("expected username 'testadmin', got '%s'", claims.Username)
	}

	if claims.Role != "ADMIN" {
		t.Errorf("expected role 'ADMIN', got '%s'", claims.Role)
	}
}

func TestTokenExpiry(t *testing.T) {
	secret := "super-secure-dev-secret-key-32b"
	svc := NewTokenService(secret, -1*time.Minute) // Already expired

	token, _, err := svc.GenerateToken("expiredadmin")
	if err != nil {
		t.Fatalf("failed to generate token: %v", err)
	}

	_, err = svc.ValidateToken(token)
	if err == nil {
		t.Fatal("expected error when validating expired token, got nil")
	}
}

func TestAuthMiddleware(t *testing.T) {
	secret := "super-secure-dev-secret-key-32b"
	svc := NewTokenService(secret, 1*time.Hour)

	protectedHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		claims, ok := r.Context().Value(AdminContextKey).(*Claims)
		if !ok || claims == nil {
			t.Fatal("expected claims in context")
		}
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("authorized"))
	})

	handler := svc.Middleware(protectedHandler)

	// Case 1: Missing header
	req := httptest.NewRequest(http.MethodGet, "/protected", nil)
	rr := httptest.NewRecorder()
	handler.ServeHTTP(rr, req)
	if rr.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 Unauthorized for missing header, got %d", rr.Code)
	}

	// Case 2: Valid Bearer token
	token, _, _ := svc.GenerateToken("admin")
	req = httptest.NewRequest(http.MethodGet, "/protected", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rr = httptest.NewRecorder()
	handler.ServeHTTP(rr, req)
	if rr.Code != http.StatusOK {
		t.Errorf("expected 200 OK with valid bearer token, got %d", rr.Code)
	}
}
