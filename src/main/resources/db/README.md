# Database migrations

We use Flyway. Migrations live in `src/main/resources/db/migration/V{n}__name.sql`
and are applied automatically on app start. Migrations are **forward-only** —
each one runs exactly once and is recorded in `flyway_schema_history`.

## Conventions

- Filenames: `V{n}__short_name.sql` (e.g. `V18__add_lead_priority.sql`)
- Versions are integers, monotonically increasing, no gaps
- Each migration is wrapped in an implicit Postgres transaction; DDL + data
  changes in the same file are atomic
- **Never** edit an applied migration. Add a new V{n+1} instead.
- Schema changes that drop columns or tables should ship in two releases:
  release N — code stops reading/writing the column; release N+1 — migration drops it.

## Rollback strategy

Flyway Community has no built-in `undo`. We use a manual convention:

1. For each `V{n}__*.sql` that ships to production, create a sibling
   `src/main/resources/db/undo/U{n}__*.sql` with the inverse statements.
2. The undo script must also remove the row from `flyway_schema_history`
   so the migration can be re-applied later.
3. Run with `deploy/scripts/rollback.sh [version]`.

Example: `V17__message_templates.sql` creates a table; `U17__message_templates.sql`
drops it and removes the history row.

## When to roll back

- Bad migration that broke production → roll back, redeploy the previous app
  image, fix the migration as V{n+1}, redeploy.
- Failed deploy where the migration succeeded but the app didn't start →
  most of the time the migration is fine; downgrade the app image. Only roll
  back if the schema change is incompatible with the previous code.

## When NOT to roll back

- After significant new data has been written that depends on the schema
  change. In that case, **roll forward** with a corrective migration.

## Day-1 testing

Every PR's CI runs `mvn verify` which applies all migrations against H2 with
PostgreSQL mode. This catches syntax errors but not Postgres-specific issues
(`tsvector`, partial indexes, etc.). For those, run integration tests with
Testcontainers locally before merging.

## Backups before any destructive migration

Before applying a `DROP COLUMN`, `DROP TABLE`, or `ALTER TYPE` migration in
production:

```bash
docker exec customer_recovery_system-postgres-1 \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" | gzip > backup-pre-migration.sql.gz
```

Keep it for at least 7 days.
