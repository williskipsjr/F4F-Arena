package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"sort"
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
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
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

	files, err := os.ReadDir(migrationsDir)
	if err != nil {
		log.Fatalf("Failed to read migrations directory %s: %v", migrationsDir, err)
	}

	// Fetch applied migrations
	rows, err := conn.Query(ctx, "SELECT version FROM schema_migrations")
	if err != nil {
		log.Fatalf("Failed querying applied migrations: %v", err)
	}
	applied := make(map[string]bool)
	for rows.Next() {
		var v string
		if err := rows.Scan(&v); err == nil {
			applied[v] = true
		}
	}
	rows.Close()

	if action == "up" {
		var upFiles []string
		for _, f := range files {
			if strings.HasSuffix(f.Name(), ".up.sql") {
				upFiles = append(upFiles, f.Name())
			}
		}
		sort.Strings(upFiles)

		for _, fileName := range upFiles {
			version := strings.TrimSuffix(fileName, ".up.sql")
			if applied[version] {
				log.Printf("[Migrate] Skipping already applied migration: %s", version)
				continue
			}

			filePath := filepath.Join(migrationsDir, fileName)
			content, err := os.ReadFile(filePath)
			if err != nil {
				log.Fatalf("Failed to read file %s: %v", filePath, err)
			}

			tx, err := conn.Begin(ctx)
			if err != nil {
				log.Fatalf("Failed to begin transaction: %v", err)
			}

			log.Printf("[Migrate] Applying %s...", fileName)
			if _, err := tx.Exec(ctx, string(content)); err != nil {
				_ = tx.Rollback(ctx)
				log.Fatalf("Failed applying %s: %v", fileName, err)
			}

			if _, err := tx.Exec(ctx, "INSERT INTO schema_migrations (version) VALUES ($1)", version); err != nil {
				_ = tx.Rollback(ctx)
				log.Fatalf("Failed recording version %s: %v", version, err)
			}

			if err := tx.Commit(ctx); err != nil {
				log.Fatalf("Failed to commit %s: %v", fileName, err)
			}
			log.Printf("[Migrate] Successfully applied %s", version)
		}
	} else {
		// Rollback latest migration
		var appliedVersions []string
		for v := range applied {
			appliedVersions = append(appliedVersions, v)
		}
		sort.Sort(sort.Reverse(sort.StringSlice(appliedVersions)))

		if len(appliedVersions) == 0 {
			fmt.Println("No migrations to rollback.")
			return
		}

		latest := appliedVersions[0]
		downFile := filepath.Join(migrationsDir, latest+".down.sql")
		content, err := os.ReadFile(downFile)
		if err != nil {
			log.Fatalf("Failed to read down migration %s: %v", downFile, err)
		}

		tx, err := conn.Begin(ctx)
		if err != nil {
			log.Fatalf("Failed to begin transaction: %v", err)
		}

		log.Printf("[Migrate] Rolling back %s...", latest)
		if _, err := tx.Exec(ctx, string(content)); err != nil {
			_ = tx.Rollback(ctx)
			log.Fatalf("Failed rolling back %s: %v", latest, err)
		}

		if _, err := tx.Exec(ctx, "DELETE FROM schema_migrations WHERE version = $1", latest); err != nil {
			_ = tx.Rollback(ctx)
			log.Fatalf("Failed removing version record %s: %v", latest, err)
		}

		if err := tx.Commit(ctx); err != nil {
			log.Fatalf("Failed to commit rollback %s: %v", latest, err)
		}
		log.Printf("[Migrate] Successfully rolled back %s", latest)
	}

	fmt.Printf("Migration '%s' process completed!\n", action)
}
