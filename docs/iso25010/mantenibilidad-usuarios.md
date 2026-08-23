# ISO/IEC 25010 — Mantenibilidad: usuarios-service

## Qué se midió

La guía de la Entrega 4 exige, para la característica de **Mantenibilidad**, dos métricas objetivas por
microservicio:

1. **Complejidad ciclomática media por método** (umbral: < 10).
2. **Cobertura de pruebas** (umbral: >= 70%).

Ambas se midieron sobre `services/usuarios-service` con las herramientas ya usadas en el resto del
proyecto: Checkstyle (módulo `CyclomaticComplexity`) y JaCoCo, ambos configurados como plugins Maven en
`pom.xml`.

## Cómo — complejidad ciclomática

**Configuración agregada:** `services/usuarios-service/checkstyle.xml` (nuevo archivo) con el módulo
`CyclomaticComplexity` en `max=10`, y el plugin `maven-checkstyle-plugin` (versión 3.6.0, misma versión que
`academico-laboratorios-service`) agregado a `services/usuarios-service/pom.xml`.

**Comando exacto ejecutado** (desde `services/usuarios-service`):

```
./mvnw.cmd checkstyle:check
```

Con `max=10` en el `checkstyle.xml` real (el que queda commiteado), la salida no reporta ninguna
violación `[CyclomaticComplexity]` — el build falla solo por 38 violaciones preexistentes de
`LineLength` y `AvoidStarImport` (no relacionadas con complejidad, fuera del alcance de esta tarea).

Para conocer la complejidad **real** de cada método (no solo si supera o no el umbral), se corrió
además una medición puntual con una copia temporal del `checkstyle.xml` con `max=0` (severidad `info`,
sin afectar el archivo final commiteado), lo que hace que Checkstyle reporte la complejidad real de cada
método analizado:

```
./mvnw.cmd checkstyle:check -Dcheckstyle.violationSeverity=info -Dcheckstyle.failOnViolation=false
```

## Resultado — complejidad ciclomática

- **Métodos analizados:** 445 (todo `src/main/java`, incluye getters/setters de las entidades JPA y
  clases de dominio, no solo lógica de negocio).
- **Métodos que superan complejidad 10:** **0**.
- **Complejidad máxima encontrada:** **6**, en
  `infrastructure/persistence/specification/PerfilSpecification.java:102`.
- **Complejidad media:** **≈1.16** (445 métodos, mayoría getters/setters/constructores con
  complejidad 1; los métodos de negocio en `*ServiceImpl` rondan complejidad 3–5).

**Comparación contra el umbral de la guía (< 10 de complejidad media):** ✅ cumple, con amplio margen
(1.16 vs. el umbral de 10, y ningún método individual supera 10; el máximo individual, 6, tampoco lo
supera).

## Cómo — cobertura de pruebas (JaCoCo)

JaCoCo ya estaba configurado en `services/usuarios-service/pom.xml` (plugin `jacoco-maven-plugin`
0.8.13, con `prepare-agent`, `report` y una regla `check` de `LINE COVEREDRATIO >= 0.70` en fase
`verify`), no se modificó.

**Comando exacto ejecutado** (desde `services/usuarios-service`):

```
./mvnw.cmd clean verify -Dtest='!CockroachFlywayIntegrationTest' -DfailIfNoTests=false
```

Se excluyó únicamente `CockroachFlywayIntegrationTest` porque requiere Docker (Testcontainers levanta un
contenedor real de CockroachDB) y Docker no estaba disponible al momento de la ejecución
(`docker info` responde que no se pudo encontrar un entorno Docker válido). El resto de la suite —136
tests, entre ellos unitarios de `application/service`, `infrastructure/persistence` y
`presentation/controller`— se ejecutó sin exclusiones ni cambios de código.

## Resultado — cobertura

```
Tests run: 136, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jacoco:0.8.13:check (check) @ usuarios-service ---
[INFO] All coverage checks have been met.
```

De `target/site/jacoco/jacoco.csv` (agregado por líneas, todas las clases del bundle
`usuarios-service`):

- **Cobertura de líneas (LINE):** **83.81%** (1123 líneas cubiertas / 1340 líneas totales).
- Cobertura de instrucciones (INSTRUCTION): 83.36% (referencia adicional, no es la métrica de la regla).
- Cobertura de ramas (BRANCH): 60.53% (referencia adicional, no es la métrica de la regla).

**Comparación contra el umbral de la guía (>= 70% de cobertura):** ✅ cumple — 83.81% de cobertura de
líneas, 13.81 puntos por encima del mínimo exigido. La propia regla JaCoCo del `pom.xml`
(`LINE COVEREDRATIO >= 0.70`) también lo confirma automáticamente en cada `mvn verify`.

**Nota sobre `CockroachFlywayIntegrationTest`:** al quedar excluida de esta medición por no contar con
Docker disponible, la cobertura obtenida en un entorno CI con Docker (que sí puede levantar
Testcontainers) puede ser igual o levemente superior a 83.81%, nunca inferior, ya que esa clase solo
suma cobertura adicional sobre `PerfilRepositoryAdapter` y las migraciones Flyway.

## Conclusión

Ambas métricas de Mantenibilidad exigidas por la guía se cumplen según lo medido con las herramientas
configuradas, sin necesidad de refactorizar lógica de negocio:

| Métrica | Umbral guía | Resultado | Cumple |
|---|---|---|---|
| Complejidad ciclomática media | < 10 | ≈1.16 (máx. individual: 6) | ✅ |
| Cobertura de líneas | >= 70% | 83.81% | ✅ |
