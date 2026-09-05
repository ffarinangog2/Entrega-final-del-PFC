# ISO/IEC 25010 — Mantenibilidad

**Fecha de revisión:** 2026-09-04

**HEAD auditado:** `cd61b64325480cbe132af7e56328f7fa5d8b99ef`

## Metodología

Se contrastaron las reglas JaCoCo de cada POM, Vitest/V8, JaCoCo Android, Checkstyle, ESLint/lint y los jobs del workflow con los resultados accesibles del HEAD. Una configuración de umbral se informa como gate, no como porcentaje ejecutado. Cuando no existe reporte cuantitativo actual de complejidad o cobertura, se declara no concluyente.

No se ejecutaron suites pesadas. Los runs consultados fueron [push 33838233548](https://github.com/ffarinangog2/Entrega-final-del-PFC/actions/runs/33838233548) y [pull request 33838236394](https://github.com/ffarinangog2/Entrega-final-del-PFC/actions/runs/33838236394).

## Resultados actuales

| Componente | Cobertura disponible | Umbral configurado | Complejidad disponible | Gate CI del HEAD | Estado sustentable |
|---|---|---|---|---|---|
| Auth | El job `mvn verify` del HEAD pasó; por la regla implica líneas >=70%, sin porcentaje persistido | JaCoCo LINE >=70% | Sin medición global versionada | Backend: verde | PARCIAL |
| Usuarios | Medición histórica: 83,81% líneas; no es atribuible al HEAD actual | JaCoCo LINE >=70% | Histórica: media 1,16 y máximo 6; Checkstyle no es gate CI para este servicio | Backend: rojo | NO CONCLUYENTE |
| Académico | El job `mvn verify` del HEAD pasó; implica líneas >=70%, sin porcentaje persistido | JaCoCo LINE >=70% | Checkstyle configurado y lint CI verde; sin media global persistida | Backend y Checkstyle: verdes | PARCIAL |
| Reservas | Sin porcentaje válido del HEAD porque `mvn verify` falló | JaCoCo LINE >=80%; BRANCH >=48% | Sin medición global versionada | Backend: rojo | NO CONCLUYENTE |
| Gateway | El job `mvn verify` del HEAD pasó; implica líneas >=70%, sin porcentaje persistido | JaCoCo LINE >=70% | Sin medición global versionada | Backend: verde | PARCIAL |
| Web | Reporte local actual: líneas 86,55%, statements 82,96%, functions 74,81%, branches 70,33%; `Test web` y ESLint verdes en CI | Vitest: 70% en líneas, statements, functions y branches | Sin complejidad ciclomática global | Web y ESLint: verdes | PARCIAL |
| Android | Reporte JaCoCo actual: líneas 45,69%, branches 12,92% | No existe umbral JaCoCo que falle el build | Sin complejidad global | Unit tests/lint verdes; instrumentado verde en push y rojo en PR | PARCIAL |

## Interpretación

Los gates de Auth, Académico, Gateway y Web acreditan sus mínimos configurados en el run de push. No se publica el reporte JaCoCo Backend como artifact, por lo que no se asignan porcentajes exactos. Usuarios y Reservas fallaron en `mvn verify`; aunque sus causas puedan ser pruebas concretas y no cobertura, un job rojo no demuestra cumplimiento del HEAD.

La cifra de Usuarios (83,81% de líneas, complejidad media 1,16 y máximo 6) se conserva como medición histórica documentada, no como resultado actual. En CI solo Académico ejecuta Checkstyle desde el job `lint`; Usuarios tiene configuración Checkstyle, pero el workflow actual no la invoca explícitamente.

Web supera sus cuatro umbrales en el reporte actual disponible. Android produce y publica cobertura, pero carece de gate mínimo y su cobertura de líneas 45,69% está por debajo del objetivo documental de 70%; no debe presentarse como cumplimiento.

## Conclusión

**Mantenibilidad: PARCIAL.** Hay automatización y umbrales efectivos en Backend y Web, lint en Web/Android y Checkstyle en Académico. No obstante, faltan reportes cuantitativos persistidos para todo Backend, no existe complejidad comparable en todos los módulos, Android permanece bajo 70% y dos servicios Backend no completaron su gate en el HEAD. Una conclusión global de cumplimiento no está sustentada.
