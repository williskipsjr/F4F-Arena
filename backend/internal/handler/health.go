package handler

import (
	"context"
	"encoding/json"
	"net/http"
	"time"
)

type DBChecker interface {
	Ping(ctx context.Context) error
}

type HealthResponse struct {
	Status    string    `json:"status"`
	Version   string    `json:"version"`
	Service   string    `json:"service"`
	Database  string    `json:"database"`
	Timestamp time.Time `json:"timestamp"`
}

type HealthHandler struct {
	db      DBChecker
	version string
}

func NewHealthHandler(db DBChecker, version string) *HealthHandler {
	return &HealthHandler{
		db:      db,
		version: version,
	}
}

func (h *HealthHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
	defer cancel()

	dbStatus := "connected"
	if h.db != nil {
		if err := h.db.Ping(ctx); err != nil {
			dbStatus = "disconnected"
		}
	} else {
		dbStatus = "unconfigured"
	}

	resp := HealthResponse{
		Status:    "healthy",
		Version:   h.version,
		Service:   "f4f-arena-api",
		Database:  dbStatus,
		Timestamp: time.Now().UTC(),
	}

	w.Header().Set("Content-Type", "application/json")
	if dbStatus == "disconnected" {
		w.WriteHeader(http.StatusServiceUnavailable)
	} else {
		w.WriteHeader(http.StatusOK)
	}

	_ = json.NewEncoder(w).Encode(resp)
}
