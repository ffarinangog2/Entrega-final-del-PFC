# Continuous Deployment a una VM Linux

El despliegue queda preparado con este flujo, pero permanece deshabilitado hasta
que exista una VM y la variable de repositorio `CD_ENABLED` sea `true`:

```text
CI -> Integration/Playwright/Locust -> GHCR -> SSH -> Docker Compose -> Smoke test
```

El job `deploy` solo puede ejecutarse para un `push` a `main`, después de que las
seis variantes de `build-images` terminen correctamente. La versión desplegada es
siempre el SHA completo de ese workflow; no se usa `latest`.

## Preparación manual de la VM

El administrador debe:

1. Instalar Docker Engine, el plugin Docker Compose v2, Git, Bash y curl.
2. Crear un usuario de despliegue sin contraseña interactiva, con acceso limitado
   por SSH y permiso para usar Docker.
3. Clonar este repositorio en un directorio dedicado y conservar `.env` fuera de Git.
4. Crear `.env` a partir de `.env.example`, reemplazando todos los ejemplos por
   secretos aleatorios de producción.
5. Configurar acceso de solo lectura a los paquetes GHCR. El token solo necesita
   permiso `read:packages`.
6. Permitir en el firewall únicamente SSH desde orígenes administrativos y los
   puertos públicos de Web/API requeridos. CockroachDB y los microservicios no se
   publican desde `docker-compose.prod.yml`.
7. Configurar TLS y un reverse proxy en los puertos 80/443 antes de exposición pública.

La VM debe conservar los volúmenes Docker de CockroachDB. El script de despliegue
no elimina volúmenes ni ejecuta `docker system prune`.

## Configuración de GitHub

Variables del repositorio:

- `CD_ENABLED`: debe ser exactamente `true` para habilitar el job.
- `SERVER_DEPLOY_PATH`: ruta absoluta del clon en la VM.

Secrets del repositorio o de un environment protegido:

- `SERVER_HOST`
- `SERVER_USER`
- `SERVER_SSH_KEY`
- `SERVER_SSH_PORT`
- `SERVER_SSH_FINGERPRINT`
- `GHCR_USERNAME`
- `GHCR_READ_TOKEN`

No se deben guardar claves SSH, tokens ni el `.env` productivo en el repositorio.
Antes de habilitar CD se recomienda mover estos secrets a un GitHub Environment
`production` con las protecciones y aprobaciones que defina el equipo.

## Ejecución remota

El workflow entra al directorio configurado, verifica que no haya cambios Git
versionados, obtiene el commit de `main`, cambia a ese SHA en modo detached, inicia
sesión de lectura en GHCR y ejecuta:

```bash
IMAGE_TAG=<SHA_APROBADO> bash scripts/deploy/deploy-vm.sh
```

El script valida el SHA y `.env`, ejecuta `docker compose pull`, levanta
`docker-compose.prod.yml`, espera `http://127.0.0.1:8080/actuator/health`, exige
estado `UP` y muestra los contenedores. Solo elimina imágenes no utilizadas con
más de siete días; nunca elimina volúmenes.

## Rollback por SHA

Para recuperar una versión anterior se selecciona un SHA cuyas seis imágenes aún
existan en GHCR y se ejecuta en la VM:

```bash
export IMAGE_TAG=<SHA_ANTERIOR>
bash scripts/deploy/deploy-vm.sh
```

El rollback vuelve a descargar las seis imágenes con ese SHA y recrea los
contenedores sin eliminar los datos persistentes. Las migraciones de base de datos
deben mantener compatibilidad hacia atrás; si una futura migración fuera
irreversible, necesitará un procedimiento específico adicional.
