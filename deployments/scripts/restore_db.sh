#!/usr/bin/env bash
set -euo pipefail

# F4F Arena PostgreSQL Restore Script
# Usage: ./restore_db.sh <path_to_backup.sql.gz>

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <path_to_backup.sql.gz>"
  exit 1
fi

BACKUP_FILE="$1"
CONTAINER_NAME="${POSTGRES_CONTAINER:-f4f_arena_postgres_prod}"
DB_USER="${POSTGRES_USER:-f4f_admin}"
DB_NAME="${POSTGRES_DB:-f4f_arena}"

if [ ! -f "${BACKUP_FILE}" ]; then
  echo "Error: Backup file '${BACKUP_FILE}' not found."
  exit 1
fi

echo "[F4F Restore] Restoring '${BACKUP_FILE}' into '${DB_NAME}' on container '${CONTAINER_NAME}'..."
gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}"
echo "[F4F Restore] Database restore completed successfully."
