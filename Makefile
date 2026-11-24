# F4F Arena — Monorepo Automation Tasks

.PHONY: help dev-deps up down logs migrate-up migrate-down run-backend test-backend build-backend

help:
	@echo "F4F Arena Build & Development Commands:"
	@echo "  make up            - Start PostgreSQL database via Docker Compose"
	@echo "  make down          - Stop PostgreSQL database"
	@echo "  make logs          - Tail database container logs"
	@echo "  make migrate-up    - Run database migrations"
	@echo "  make migrate-down  - Rollback database migrations"
	@echo "  make run-backend   - Run Go API server locally"
	@echo "  make test-backend  - Run backend unit tests"
	@echo "  make build-backend - Compile Go backend binaries"

up:
	docker compose -f deployments/docker-compose.yml up -d

down:
	docker compose -f deployments/docker-compose.yml down

logs:
	docker compose -f deployments/docker-compose.yml logs -f

migrate-up:
	cd backend && go run cmd/migrate/main.go up

migrate-down:
	cd backend && go run cmd/migrate/main.go down

run-backend:
	cd backend && go run cmd/api/main.go

test-backend:
	cd backend && go test -v -race ./...

build-backend:
	mkdir -p backend/bin
	cd backend && go build -o bin/api cmd/api/main.go
	cd backend && go build -o bin/migrate cmd/migrate/main.go
