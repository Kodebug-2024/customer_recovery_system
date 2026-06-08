#!/usr/bin/env bash
# Roll back the most recently applied Flyway migration by running its companion
# undo script from src/main/resources/db/undo/U{version}__*.sql.
#
# Usage:
#   ./deploy/scripts/rollback.sh                       # roll back last migration
#   ./deploy/scripts/rollback.sh 17                    # roll back V17 specifically
#
# Assumptions:
#   * Postgres reachable via $DB_URL or the env vars PGHOST / PGUSER / PGPASSWORD / PGDATABASE.
#   * psql installed on the operator machine, OR the script run inside the postgres container.
#   * For each Vnn migration you want to support rollback for, create a sibling
#     Unn__*.sql with the inverse statements. Free-form Flyway "undo" is a paid
#     feature; this manual approach gives you the same outcome at zero cost.

set -euo pipefail

VERSION="${1:-}"

if ! command -v psql >/dev/null 2>&1; then
  echo "psql not found. Install postgresql-client or run this inside the postgres container." >&2
  exit 1
fi

# If no version given, pick the most recently applied non-undo migration.
if [[ -z "$VERSION" ]]; then
  VERSION=$(psql -tA -c \
    "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")
  if [[ -z "$VERSION" ]]; then
    echo "No migrations applied; nothing to roll back." >&2
    exit 1
  fi
fi

UNDO_DIR="$(dirname "$0")/../../src/main/resources/db/undo"
UNDO_FILE=$(ls "$UNDO_DIR"/U"${VERSION}"__*.sql 2>/dev/null | head -n 1 || true)

if [[ -z "$UNDO_FILE" ]]; then
  echo "No undo script found for version $VERSION at $UNDO_DIR/U${VERSION}__*.sql" >&2
  echo "Create one to enable rollback for this migration." >&2
  exit 1
fi

echo "About to roll back V$VERSION using $UNDO_FILE"
read -r -p "Proceed? (yes/no) " confirm
[[ "$confirm" == "yes" ]] || { echo "Aborted."; exit 1; }

psql -v ON_ERROR_STOP=1 -f "$UNDO_FILE"
echo "Rollback of V$VERSION complete."
