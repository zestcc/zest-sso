#!/usr/bin/env bash
# ZestSSO 生产备份脚本（Linux）
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?Set MYSQL_PASSWORD}"
MYSQL_DATABASE="${MYSQL_DATABASE:-zest_sso}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
OUT_DIR="${OUT_DIR:-./backups}"
TS="$(date +%Y%m%d-%H%M%S)"
DEST="${OUT_DIR}/${TS}"

mkdir -p "$DEST"
echo "ZestSSO backup -> $DEST"

mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
  --single-transaction --routines --triggers "$MYSQL_DATABASE" \
  > "${DEST}/zest_sso.sql"
echo "MySQL: ${DEST}/zest_sso.sql"

if command -v redis-cli >/dev/null 2>&1; then
  redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" BGSAVE
  echo "Redis BGSAVE triggered"
else
  echo "WARN: redis-cli not found"
fi

echo "Backup completed: $DEST"
