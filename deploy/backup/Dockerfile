FROM alpine:3.20

RUN apk add --no-cache postgresql16-client aws-cli ca-certificates tzdata bash

COPY backup.sh /usr/local/bin/backup.sh
RUN chmod +x /usr/local/bin/backup.sh

# Run via crond. The crontab is generated at container start so it can pick
# up the BACKUP_CRON env var.
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
