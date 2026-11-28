package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"

	"github.com/williskipsjr/f4f-arena/backend/internal/auth"
	"github.com/williskipsjr/f4f-arena/backend/internal/config"
	"github.com/williskipsjr/f4f-arena/backend/internal/database"
	"github.com/williskipsjr/f4f-arena/backend/internal/handler"
	"github.com/williskipsjr/f4f-arena/backend/internal/repository/postgres"
	"github.com/williskipsjr/f4f-arena/backend/internal/service"
)

const AppVersion = "1.0.0"

func main() {
	cfg := config.Load()
	log.Printf("[F4F Arena] Initializing API service version %s (Env: %s)...", AppVersion, cfg.ServerEnv)

	// Database Connection (Graceful initialization)
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	var db *database.DB
	var err error
	db, err = database.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Printf("[F4F Arena] Warning: Database connection failed (%v). Service will run with degraded health status.", err)
	} else {
		log.Println("[F4F Arena] Successfully connected to PostgreSQL database.")
		defer db.Close()
	}

	// Token & Auth Services
	tokenService := auth.NewTokenService(cfg.JWTSecret, cfg.TokenExpiryHours)

	// Repositories & Services
	var playerHandler *handler.PlayerHandler
	var matchHandler *handler.MatchHandler
	var teamHandler *handler.TeamHandler
	var feedbackHandler *handler.FeedbackHandler
	var accountRepo *postgres.AccountRepository
	if db != nil && db.Pool != nil {
		accountRepo = postgres.NewAccountRepository(db.Pool)
		feedbackRepo := postgres.NewFeedbackRepository(db.Pool)
		feedbackHandler = handler.NewFeedbackHandler(feedbackRepo)

		playerRepo := postgres.NewPlayerRepository(db.Pool)
		playerService := service.NewPlayerService(playerRepo)
		playerHandler = handler.NewPlayerHandler(playerService)

		matchRepo := postgres.NewMatchRepository(db.Pool)
		matchService := service.NewMatchService(matchRepo)
		matchHandler = handler.NewMatchHandler(matchService).WithVoting(matchRepo, tokenService)

		teamRepo := postgres.NewTeamRepository(db.Pool)
		teamService := service.NewTeamService(matchRepo, teamRepo)
		teamHandler = handler.NewTeamHandler(teamService)
	}

	// HTTP Handlers
	healthHandler := handler.NewHealthHandler(db, AppVersion)
	authHandler := handler.NewAuthHandler(cfg.AdminUsername, cfg.AdminPassword, tokenService).WithAccountRepo(accountRepo)

	// Router Setup
	r := chi.NewRouter()

	// Global Middlewares
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(middleware.Timeout(30 * time.Second))

	// CORS Setup (allowing Android Emulator 10.0.2.2, LAN IPs, and localhost)
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type", "X-CSRF-Token", "X-Admin-Key"},
		ExposedHeaders:   []string{"Link"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	// Public Routes
	r.Get("/health", healthHandler.ServeHTTP)
	r.Get("/api/v1/app/version", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(w).Encode(map[string]interface{}{
			"version_code":  cfg.LatestAppVersionCode,
			"version_name":  cfg.LatestAppVersionName,
			"download_url":  cfg.AppDownloadURL,
			"release_notes": cfg.AppReleaseNotes,
		})
	})
	r.Post("/api/v1/auth/login", authHandler.Login)
	r.Post("/api/v1/auth/register", authHandler.Register)
	r.Post("/api/v1/auth/phone-lookup", authHandler.PhoneLookup)
	r.Post("/api/v1/auth/claim-profile", authHandler.ClaimProfile)
	r.Post("/api/v1/auth/reset-pin", authHandler.ResetPin)
	if playerHandler != nil {
		r.Get("/api/v1/players", playerHandler.List)
		r.Get("/api/v1/players/{id}", playerHandler.Get)
	}
	if matchHandler != nil {
		r.Get("/api/v1/matches", matchHandler.List)
		r.Get("/api/v1/matches/{id}", matchHandler.Get)
		r.Get("/api/v1/matches/{id}/attendees", matchHandler.GetAttendees)
		r.Get("/api/v1/matches/{id}/votes", matchHandler.GetVotes)
		r.Get("/api/v1/matches/{id}/comments", matchHandler.GetComments)
	}
	if teamHandler != nil {
		r.Get("/api/v1/matches/{id}/teams", teamHandler.GetTeams)
		r.Get("/api/v1/matches/{id}/whatsapp-export", teamHandler.GetWhatsAppExport)
	}

	// Authenticated Routes (Players + Admin)
	r.Group(func(authed chi.Router) {
		authed.Use(tokenService.Middleware)
		authed.Get("/api/v1/auth/verify", func(w http.ResponseWriter, r *http.Request) {
			claims, _ := r.Context().Value(auth.AdminContextKey).(*auth.Claims)
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusOK)
			_, _ = fmt.Fprintf(w, `{"status":"authenticated","user":"%s","role":"%s"}`, claims.Username, claims.Role)
		})
		authed.Post("/api/v1/auth/change-password", authHandler.ChangePassword)
		authed.Patch("/api/v1/auth/profile", authHandler.UpdateProfile)
		if matchHandler != nil {
			authed.Post("/api/v1/matches/{id}/vote", matchHandler.CastVote)
			authed.Post("/api/v1/matches/{id}/comments", matchHandler.PostComment)
		}
		if feedbackHandler != nil {
			authed.Post("/api/v1/feedback", feedbackHandler.Create)
		}

		// Admin-only Mutations & Diagnostics
		authed.Group(func(admin chi.Router) {
			admin.Use(tokenService.RequireAdmin)
			if feedbackHandler != nil {
				admin.Get("/api/v1/feedback", feedbackHandler.List)
			}
			if playerHandler != nil {
				admin.Post("/api/v1/players", playerHandler.Create)
				admin.Patch("/api/v1/players/{id}", playerHandler.Update)
			}
			if matchHandler != nil {
				admin.Post("/api/v1/matches", matchHandler.Create)
				admin.Patch("/api/v1/matches/{id}", matchHandler.Update)
				admin.Delete("/api/v1/matches/{id}", matchHandler.Delete)
				admin.Delete("/api/v1/matches/{id}/comments/{commentId}", matchHandler.DeleteComment)
				admin.Put("/api/v1/matches/{id}/attendees", matchHandler.SyncAttendees)
				admin.Post("/api/v1/matches/{id}/attendees/late", matchHandler.AddLateArrival)
				admin.Delete("/api/v1/matches/{id}/attendees/{playerId}", matchHandler.RemoveAttendee)
			}
			if teamHandler != nil {
				admin.Post("/api/v1/matches/{id}/generate-teams", teamHandler.GenerateTeams)
				admin.Post("/api/v1/matches/{id}/assignments/swap", teamHandler.SwapPlayers)
				admin.Post("/api/v1/matches/{id}/assignments/move", teamHandler.MovePlayer)
				admin.Post("/api/v1/matches/{id}/finalise", teamHandler.FinaliseMatch)
			}
		})
	})

	server := &http.Server{
		Addr:         ":" + cfg.ServerPort,
		Handler:      r,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Server runner in background goroutine
	go func() {
		log.Printf("[F4F Arena] HTTP server listening on port :%s", cfg.ServerPort)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("[F4F Arena] Server error: %v", err)
		}
	}()

	// Graceful Shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt, syscall.SIGTERM)
	<-quit

	log.Println("[F4F Arena] Shutting down HTTP server gracefully...")
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Fatalf("[F4F Arena] Server forced to shutdown: %v", err)
	}

	log.Println("[F4F Arena] Server stopped cleanly.")
}
