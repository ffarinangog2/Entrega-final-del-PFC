# Acta 05 — APK Android release firmado y cierre técnico E7

- **Fecha:** 12/09/2026
- **Hora:** Desde las 19:00
- **Modalidad:** Google Meet
- **Participantes:**
  - Freddy Farinango
  - Isaías Urbina
  - Iván Villamarín
  - Harold Vinueza
- **Estado:** Confirmada
- **Bloque:** Jornada posterior del mismo día al trabajo documentado en el Acta 04.

## Objetivo

Preparar y verificar un proceso reproducible para construir, firmar y publicar
el APK Android release mediante GitHub Actions.

## Temas tratados

- Protección del keystore y otros materiales privados.
- Configuración mediante nombres de GitHub Secrets, sin versionar sus valores.
- Construcción release, `zipalign`, firma y verificación con `apksigner`.
- Checksum SHA-256 y publicación controlada como artifact.

## Decisiones y acuerdos

- Mantener el keystore fuera de Git y reconstruirlo temporalmente en el runner.
- Ejecutar el job firmado únicamente en pushes autorizados a
  `feature/entrega-4` y sin introducir dependencias para otros jobs.
- Publicar únicamente el APK final y `SHA256SUMS.txt`, sin crear todavía un
  GitHub Release.

## Tareas y responsables

| Tarea | Responsable | Estado | Evidencia |
|---|---|---|---|
| Revisar la ubicación del artefacto dentro de la arquitectura del proyecto | Freddy Farinango | Completada | `apps/mobile/README.md` |
| Integrar el job de build y firma | Isaías Urbina | Completada | `.github/workflows/ci-cd.yml` |
| Verificar firma y checksum dentro del pipeline | Iván Villamarín | Completada | Run de Actions `34682703140` |
| Documentar secrets, descarga, verificación e instalación | Harold Vinueza | Completada | `README.md` y `apps/mobile/README.md` |

## Resultados de la jornada

El commit E7 incorporó la protección de materiales de firma y el job Android
release. La ejecución posterior de GitHub Actions `34682703140` finalizó
correctamente: construyó el APK, verificó su firma, comprobó el checksum y
publicó el artifact asociado al SHA del commit.

## Evidencia relacionada

- Commit E7: `afa9794242506c85124a6468e986768751902296`.
- Workflow: `.github/workflows/ci-cd.yml`.
- Documentación: `apps/mobile/README.md`.
- Ejecución posterior: GitHub Actions `34682703140`.
- Artifact: `scli-mobile-release-afa9794242506c85124a6468e986768751902296`.

## Seguimiento

El proceso firmado quedó verificado en CI. El artifact contiene
`scli-mobile-0.1.0-release.apk` y `SHA256SUMS.txt`; no se creó un GitHub Release.
