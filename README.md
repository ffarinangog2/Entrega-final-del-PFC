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
- `services/usuarios-service`: administración de usuarios.
- `services/academico-laboratorios-service`: información académica y laboratorios.
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

## Freddy: Reservas/Solicitudes y cliente móvil

Los comandos Maven del servicio se ejecutan desde `services/reservas-solicitudes-service`:

```powershell
Push-Location services/reservas-solicitudes-service
.\mvnw.cmd test
$jwtKey = [byte[]](1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 })
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKey)
.\mvnw.cmd spring-boot:run
Pop-Location
```

Con el servicio iniciado en el puerto local predeterminado, Actuator expone:

```powershell
Invoke-RestMethod http://localhost:8084/actuator/health
Invoke-WebRequest http://localhost:8084/actuator/prometheus
```

El contrato Pact Consumer + Provider se genera y verifica desde la raíz:

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\contract\verify-reservas-provider.ps1
```

La URL del API Gateway móvil debe terminar en `/` y puede configurarse mediante la
variable de entorno `SCLI_API_BASE_URL`. Los tests unitarios Android, incluidos los de
Reservas, se ejecutan así:

```powershell
Push-Location apps/mobile
$env:SCLI_API_BASE_URL = "<URL_GATEWAY>/"
.\gradlew.bat :app:testDebugUnitTest
Pop-Location
```

La carga nominal de solo lectura (50 usuarios, incorporación de 10/s y 5 minutos) se
ejecuta desde la raíz, después de instalar `tests/load/requirements.txt` y configurar
`LOCUST_HOST` para el ambiente autorizado:

```powershell
python -m pip install -r tests/load/requirements.txt
$env:LOCUST_HOST = "<URL_SERVICIO_RESERVAS>"
python -m locust -f tests/load/locustfile.py --headless --users 50 --spawn-rate 10 --run-time 5m
```

El diseño de medición, repetición y conservación de evidencia está definido en
`experimentos/protocolo-e4.md`. Ese protocolo no constituye resultados ISO ejecutados.
