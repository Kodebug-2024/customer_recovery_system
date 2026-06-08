#!/bin/sh
set -eu

CRON_EXPR="${BACKUP_CRON:-0 3 * * *}"   # default: 03:00 UTC nightly

echo "$CRON_EXPR /usr/local/bin/backup.sh >> /var/log/backup.log 2>&1" > /etc/crontabs/root

# Make sure the log file exists so 'tail -f' works.
touch /var/log/backup.log

# Run a backup immediately on startup if requested (helpful in dev).
if [ "${BACKUP_ON_START:-false}" = "true" ]; then
  echo "[backup] running initial backup on startup"
  /usr/local/bin/backup.sh || echo "[backup] initial backup failed"
fi

# Tail the log to keep PID 1 alive and surface backup output to docker logs.
crond -f -l 8 &
exec tail -F /var/log/backup.log
