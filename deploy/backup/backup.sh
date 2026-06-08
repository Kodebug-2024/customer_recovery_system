#!/bin/sh
# Nightly Postgres backup. Dumps the database and uploads the gzipped result
# to S3 (or any S3-compatible store). Keeps a local copy under /backups so
# you can restore quickly if S3 is unreachable.
#
# Required env:
#   PGHOST, PGUSER, PGPASSWORD, PGDATABASE
#   S3_BUCKET                 (e.g. my-crm-backups)
#   S3_PREFIX                 (e.g. crm/prod)  - default: crm
#   AWS_REGION
#   AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY  (or use IAM role on EC2/ECS)
# Optional:
#   S3_ENDPOINT_URL           (for S3-compatible: Backblaze B2, Cloudflare R2, MinIO)
#   RETENTION_DAYS            (default 30 — local files older than this are removed)

set -eu

: "${PGHOST:?PGHOST required}"
: "${PGUSER:?PGUSER required}"
: "${PGPASSWORD:?PGPASSWORD required}"
: "${PGDATABASE:?PGDATABASE required}"
: "${S3_BUCKET:?S3_BUCKET required}"
: "${AWS_REGION:?AWS_REGION required}"

S3_PREFIX="${S3_PREFIX:-crm}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

ts=$(date -u +%Y%m%dT%H%M%SZ)
out_dir="/backups"
mkdir -p "$out_dir"
out_file="$out_dir/${PGDATABASE}-${ts}.sql.gz"

echo "[backup] $(date -u) starting backup of $PGDATABASE"

# pg_dump → gzip directly to disk so we don't buffer the whole DB in RAM.
pg_dump --no-owner --no-acl "$PGDATABASE" | gzip -9 > "$out_file"

bytes=$(stat -c%s "$out_file" 2>/dev/null || stat -f%z "$out_file")
echo "[backup] dump complete: $out_file ($bytes bytes)"

s3_uri="s3://${S3_BUCKET}/${S3_PREFIX}/${PGDATABASE}/${ts}.sql.gz"
extra_args=""
[ -n "${S3_ENDPOINT_URL:-}" ] && extra_args="--endpoint-url $S3_ENDPOINT_URL"

aws s3 cp $extra_args --region "$AWS_REGION" "$out_file" "$s3_uri" \
    --storage-class STANDARD_IA \
    --metadata "source=crm-backup,db=$PGDATABASE"

echo "[backup] uploaded to $s3_uri"

# Local rotation. S3 lifecycle should handle long-term rotation in the bucket.
find "$out_dir" -name "${PGDATABASE}-*.sql.gz" -type f -mtime +"$RETENTION_DAYS" -delete

echo "[backup] $(date -u) done"
