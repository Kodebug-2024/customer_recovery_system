#!/bin/sh
# Bootstrap entrypoint: optionally fetch secrets from an external manager
# before exec'ing the Spring Boot app. The default path (no provider) just
# runs the app — so this is fully backward compatible with plain env vars.
#
# Selection: set SECRETS_PROVIDER to one of:
#   doppler          — requires DOPPLER_TOKEN
#   aws-secrets      — requires AWS_REGION + AWS_SECRETS_NAMES (comma-separated)
#   vault            — requires VAULT_ADDR + VAULT_TOKEN + VAULT_SECRET_PATH
#   sops-file        — decrypts /run/secrets/secrets.enc.env via sops
#   (unset / "none") — no-op, use existing env
#
# All providers write a flat KEY=VALUE file to $TMP_ENV (default /tmp/secrets.env)
# then we `set -a` + source it so the JVM sees them as env vars.

set -eu

TMP_ENV="${TMP_ENV:-/tmp/secrets.env}"
PROVIDER="${SECRETS_PROVIDER:-none}"

fetch_doppler() {
  if ! command -v doppler >/dev/null 2>&1; then
    echo "[secrets] doppler CLI not installed; skipping" >&2; return 0
  fi
  : "${DOPPLER_TOKEN:?DOPPLER_TOKEN required for doppler provider}"
  doppler secrets download --no-file --format env > "$TMP_ENV"
}

fetch_aws() {
  if ! command -v aws >/dev/null 2>&1; then
    echo "[secrets] aws CLI not installed; skipping" >&2; return 0
  fi
  : "${AWS_REGION:?AWS_REGION required for aws-secrets provider}"
  : "${AWS_SECRETS_NAMES:?AWS_SECRETS_NAMES (comma-separated) required}"
  : > "$TMP_ENV"
  for name in $(echo "$AWS_SECRETS_NAMES" | tr ',' ' '); do
    aws secretsmanager get-secret-value --region "$AWS_REGION" \
      --secret-id "$name" --query SecretString --output text \
      | python3 -c 'import sys,json
for k,v in json.load(sys.stdin).items():
    print(f"{k}={v}")' >> "$TMP_ENV"
  done
}

fetch_vault() {
  if ! command -v vault >/dev/null 2>&1; then
    echo "[secrets] vault CLI not installed; skipping" >&2; return 0
  fi
  : "${VAULT_ADDR:?VAULT_ADDR required for vault provider}"
  : "${VAULT_TOKEN:?VAULT_TOKEN required for vault provider}"
  : "${VAULT_SECRET_PATH:?VAULT_SECRET_PATH required for vault provider}"
  vault kv get -format=json "$VAULT_SECRET_PATH" \
    | python3 -c 'import sys,json
data = json.load(sys.stdin)["data"]["data"]
for k,v in data.items():
    print(f"{k}={v}")' > "$TMP_ENV"
}

fetch_sops() {
  if ! command -v sops >/dev/null 2>&1; then
    echo "[secrets] sops not installed; skipping" >&2; return 0
  fi
  sops -d /run/secrets/secrets.enc.env > "$TMP_ENV"
}

case "$PROVIDER" in
  doppler)     fetch_doppler ;;
  aws-secrets) fetch_aws ;;
  vault)       fetch_vault ;;
  sops-file)   fetch_sops ;;
  none|"")     ;;  # no-op
  *) echo "[secrets] unknown SECRETS_PROVIDER='$PROVIDER'" >&2 ;;
esac

if [ -f "$TMP_ENV" ]; then
  echo "[secrets] loading $(wc -l < "$TMP_ENV") values from $PROVIDER"
  set -a; . "$TMP_ENV"; set +a
  rm -f "$TMP_ENV"
fi

exec java -jar /app/app.jar
