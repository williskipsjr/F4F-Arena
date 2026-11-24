package handler

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
)

type mockDBChecker struct {
	pingErr error
}

func (m *mockDBChecker) Ping(ctx context.Context) error {
	return m.pingErr
}

func TestHealthHandler_Healthy(t *testing.T) {
	mockDB := &mockDBChecker{pingErr: nil}
	handler := NewHealthHandler(mockDB, "1.0.0")

	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rr := httptest.NewRecorder()

	handler.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rr.Code)
	}

	var resp HealthResponse
	if err := json.NewDecoder(rr.Body).Decode(&resp); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}

	if resp.Status != "healthy" {
		t.Errorf("expected status 'healthy', got '%s'", resp.Status)
	}
	if resp.Database != "connected" {
		t.Errorf("expected database 'connected', got '%s'", resp.Database)
	}
	if resp.Service != "f4f-arena-api" {
		t.Errorf("expected service 'f4f-arena-api', got '%s'", resp.Service)
	}
}

func TestHealthHandler_DBDisconnected(t *testing.T) {
	mockDB := &mockDBChecker{pingErr: errors.New("connection refused")}
	handler := NewHealthHandler(mockDB, "1.0.0")

	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rr := httptest.NewRecorder()

	handler.ServeHTTP(rr, req)

	if rr.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected status 503, got %d", rr.Code)
	}

	var resp HealthResponse
	if err := json.NewDecoder(rr.Body).Decode(&resp); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}

	if resp.Database != "disconnected" {
		t.Errorf("expected database 'disconnected', got '%s'", resp.Database)
	}
}
