#!/usr/bin/env bash
set -euo pipefail

# F4F Arena Automated PostgreSQL Backup Script
# Usage: ./backup_db.sh [backup_dir]

BACKUP_DIR="${1:-./backups}"
RETENTION_DAYS=14
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
CONTAINER_NAME="${POSTGRES_CONTAINER:-f4f_arena_postgres_prod}"
DB_USER="${POSTGRES_USER:-f4f_admin}"
DB_NAME="${POSTGRES_DB:-f4f_arena}"
BACKUP_FILE="${BACKUP_DIR}/f4f_arena_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

echo "[F4F Backup] Starting backup of database '${DB_NAME}' from container '${CONTAINER_NAME}'..."
docker exec "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists | gzip > "${BACKUP_FILE}"

echo "[F4F Backup] Backup saved to ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"

# Prune backups older than RETENTION_DAYS
find "${BACKUP_DIR}" -name "f4f_arena_*.sql.gz" -type f -mtime +${RETENTION_DAYS} -delete
echo "[F4F Backup] Cleaned up backups older than ${RETENTION_DAYS} days."
