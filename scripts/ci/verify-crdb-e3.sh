#!/usr/bin/env bash

set -Eeuo pipefail

readonly EXPECTED_NODES=3
readonly EXPECTED_TABLES=8

wait_for_health() {
  local service="$1"
  local container
  local health

  for _ in {1..60}; do
    container="$(docker compose ps -q "$service")"
    if [[ -n "$container" ]]; then
      health="$(docker inspect --format '{{.State.Health.Status}}' "$container" 2>/dev/null || true)"
      if [[ "$health" == "healthy" ]]; then
        printf '%s is healthy\n' "$service"
        return 0
      fi
    fi
    sleep 5
  done

  printf 'Timed out waiting for %s\n' "$service" >&2
  docker compose ps
  return 1
}

sql() {
  docker compose exec -T crdb-e3-1 cockroach sql \
    --certs-dir=/cockroach/cockroach-certs \
    --host=localhost:26257 \
    --database=reservas_db \
    --format=tsv \
    --execute="SET allow_unsafe_internals = true; $1"
}

docker compose up -d --build reservas-solicitudes-service

for service in crdb-e3-1 crdb-e3-2 crdb-e3-3 reservas-solicitudes-service; do
  wait_for_health "$service"
done

sql "SELECT 1;" | grep -qx '1'

node_count="$(sql "SELECT count(*) FROM crdb_internal.kv_node_status;" | tail -n 1)"
if [[ "$node_count" != "$EXPECTED_NODES" ]]; then
  printf 'Expected %s CockroachDB nodes, found %s\n' "$EXPECTED_NODES" "$node_count" >&2
  exit 1
fi

database_count="$(sql "SELECT count(*) FROM [SHOW DATABASES] WHERE database_name = 'reservas_db';" | tail -n 1)"
if [[ "$database_count" != "1" ]]; then
  printf 'Database reservas_db was not initialized\n' >&2
  exit 1
fi

table_count="$(sql "
  SELECT count(*)
  FROM information_schema.tables
  WHERE table_schema = 'public'
    AND table_name IN (
      'solicitudes_reserva',
      'reservas',
      'historial_solicitudes',
      'bloqueos_agenda',
      'configuraciones_reserva',
      'idempotencia_aprobaciones',
      'idempotencia_creacion_solicitudes',
      'mutex_agenda'
    );
" | tail -n 1)"
if [[ "$table_count" != "$EXPECTED_TABLES" ]]; then
  printf 'Expected %s migrated tables, found %s\n' "$EXPECTED_TABLES" "$table_count" >&2
  exit 1
fi

flyway_version="$(sql "SELECT max(version) FROM flyway_schema_history WHERE success;" | tail -n 1)"
if [[ "$flyway_version" != "4" ]]; then
  printf 'Expected Flyway schema version 4, found %s\n' "$flyway_version" >&2
  exit 1
fi

zone_configuration="$(sql "SHOW ZONE CONFIGURATION FOR RANGE default;")"
if ! grep -Eq 'num_replicas([[:space:]]*=[[:space:]]*|:[[:space:]]*)3' <<< "$zone_configuration"; then
  printf 'Default range is not configured with num_replicas=3\n' >&2
  printf '%s\n' "$zone_configuration" >&2
  exit 1
fi

# Basic availability check: the cluster must continue accepting SQL with one
# node stopped, then return to three healthy nodes.
docker compose stop crdb-e3-3
sql "SELECT 1;" | grep -qx '1'
docker compose start crdb-e3-3
wait_for_health crdb-e3-3

printf 'CockroachDB E3 validation completed successfully\n'
