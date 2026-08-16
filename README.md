# Sistema de Control de Laboratorios e Infraestructura

# Entrega 3

## Requisitos

- Docker con soporte para Docker Compose.
- Un archivo `.env` en la raíz basado en `.env.example`.
- Los puertos configurados para los tres nodos CockroachDB E3 deben estar disponibles.

## Ejecución

Desde la raíz del repositorio:

```text
docker compose up -d
```

## Estructura general

- `auth-service`: autenticación.
- `usuarios-service`: administración de usuarios.
- `academico-laboratorios-service`: información académica y laboratorios.
- `reservas-solicitudes-service`: solicitudes y reservas.
- `frontend`: interfaz de usuario.
- `docker-compose.yml`: servicios y clúster CockroachDB E3.
- `.github/workflows/ci.yml`: trabajos `build-test` y `crdb-tests`.
- `db/schema.sql`: archivo pendiente del Paso 2; todavía no forma parte del repositorio.

## Clúster CockroachDB E3

La infraestructura de la Entrega 3 define tres nodos:

- `crdb-e3-1`
- `crdb-e3-2`
- `crdb-e3-3`

Los nodos comparten la red `scli-network`, mantienen datos en volúmenes
independientes y se descubren mediante `--join`.

La preparación de `crdb-e3-init` está documentada en `docker-compose.yml`, pero
permanece deshabilitada. Su inicialización y la carga de `db/schema.sql` dependen
del Paso 2 de Freddy.
