package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/williskipsjr/f4f-arena/backend/internal/config"
)

func main() {
	if len(os.Args) < 2 {
		log.Fatal("Usage: go run cmd/migrate/main.go [up|down]")
	}

	action := strings.ToLower(os.Args[1])
	if action != "up" && action != "down" {
		log.Fatalf("Invalid migration action '%s'. Use 'up' or 'down'.", action)
	}

	cfg := config.Load()
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	conn, err := pgx.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("Failed to connect to database for migration: %v", err)
	}
	defer conn.Close(ctx)

	// Ensure schema_migrations table exists
	_, err = conn.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS schema_migrations (
			version VARCHAR(50) PRIMARY KEY,
			applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
		);
	`)
	if err != nil {
		log.Fatalf("Failed to verify schema_migrations table: %v", err)
	}

	migrationsDir := "migrations"
	if _, err := os.Stat(migrationsDir); os.IsNotExist(err) {
		migrationsDir = filepath.Join("backend", "migrations")
	}

	var targetFile string
	if action == "up" {
		targetFile = filepath.Join(migrationsDir, "000001_initial_schema.up.sql")
	} else {
		targetFile = filepath.Join(migrationsDir, "000001_initial_schema.down.sql")
	}

	content, err := os.ReadFile(targetFile)
	if err != nil {
		log.Fatalf("Failed to read migration file %s: %v", targetFile, err)
	}

	tx, err := conn.Begin(ctx)
	if err != nil {
		log.Fatalf("Failed to begin transaction: %v", err)
	}
	defer tx.Rollback(ctx)

	log.Printf("[Migrate] Executing migration: %s...", targetFile)
	if _, err := tx.Exec(ctx, string(content)); err != nil {
		log.Fatalf("Failed executing migration SQL: %v", err)
	}

	version := "000001_initial_schema"
	if action == "up" {
		_, err = tx.Exec(ctx, `INSERT INTO schema_migrations (version) VALUES ($1) ON CONFLICT (version) DO NOTHING`, version)
	} else {
		_, err = tx.Exec(ctx, `DELETE FROM schema_migrations WHERE version = $1`, version)
	}
	if err != nil {
		log.Fatalf("Failed to update schema_migrations version: %v", err)
	}

	if err := tx.Commit(ctx); err != nil {
		log.Fatalf("Failed to commit migration transaction: %v", err)
	}

	fmt.Printf("Migration '%s' executed successfully!\n", action)
}
