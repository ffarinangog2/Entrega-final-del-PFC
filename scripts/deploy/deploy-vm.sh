#!/usr/bin/env bash

set -Eeuo pipefail

readonly COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
readonly ENV_FILE="${ENV_FILE:-.env}"
readonly HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"

if [[ -z "${IMAGE_TAG:-}" ]]; then
  printf 'IMAGE_TAG is required and must be the Git SHA approved by CI\n' >&2
  exit 1
fi

if [[ ! "$IMAGE_TAG" =~ ^[0-9a-f]{40}$ ]]; then
  printf 'IMAGE_TAG must be a full 40-character Git SHA\n' >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  printf 'Compose file not found: %s\n' "$COMPOSE_FILE" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  printf 'Environment file not found: %s\n' "$ENV_FILE" >&2
  exit 1
fi

compose=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

"${compose[@]}" config --quiet
"${compose[@]}" pull
"${compose[@]}" up -d --remove-orphans

health_body=""
for _ in {1..60}; do
  if health_body="$(curl --fail --silent --show-error "$HEALTH_URL" 2>/dev/null)" \
    && grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<< "$health_body"; then
    printf 'API Gateway is healthy at %s\n' "$HEALTH_URL"
    "${compose[@]}" ps
    docker image prune --all --force --filter 'until=168h'
    exit 0
  fi
  sleep 5
done

printf 'API Gateway did not become healthy at %s\n' "$HEALTH_URL" >&2
printf 'Last health response: %s\n' "$health_body" >&2
"${compose[@]}" ps
"${compose[@]}" logs --tail=200 api-gateway
exit 1
