# Contratos OpenAPI de SCLI

Esta carpeta contiene los contratos de los cuatro servicios HTTP y de la fachada del API Gateway. Los snapshots corresponden al código de `feature/entrega-4` en el SHA `cef50709177eee2c41b53f0000f9d936ddb373df`.

| Archivo | Alcance | Productor |
|---|---|---|
| `auth-service-openapi.json` | API de autenticación y administración interna | Extracción determinista de controladores y DTO de `auth-service` |
| `usuarios-service-openapi.json` | API externa e interna de usuarios | Extracción determinista de controladores y DTO de `usuarios-service` |
| `academico-laboratorios-service-openapi.json` | API externa e interna académica | Extracción determinista de controladores y DTO de `academico-laboratorios-service` |
| `reservas-solicitudes-service-openapi.json` | Reservas, solicitudes, agenda, asistencia y planificación | Extracción determinista de controladores y DTO de `reservas-solicitudes-service` |
| `api-gateway-openapi.json` | Fachada que el Gateway enruta hacia los servicios | Composición de los contratos anteriores según `GatewayRoutes.java` |

## Producción reproducible

Los servicios requieren infraestructura externa para arrancar y el repositorio no define un perfil común que permita obtener `/v3/api-docs` de los cuatro de forma aislada. Para evitar cambios funcionales o snapshots dependientes del entorno, los contratos se producen con Python estándar desde las anotaciones Spring MVC y los DTO vigentes:

```bash
python scripts/validar-contratos-openapi.py --generate
```

El productor descubre los controladores en `src/main/java`, combina `@RequestMapping` con `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping` y `@DeleteMapping`, y obtiene parámetros, cuerpos y tipos de respuesta de las firmas Java. También extrae las propiedades de records, clases DTO y enums. No toma endpoints de documentos históricos.

La generación crea OpenAPI 3.1.0. La versión de Reservas es 1.1.0, de acuerdo con su `OpenApiConfig`; los restantes contratos comienzan en 1.0.0 como versión documental de sus snapshots.

Para validar sin escribir archivos:

```bash
python scripts/validar-contratos-openapi.py
```

La validación comprueba JSON, campos básicos, métodos HTTP, `operationId`, referencias locales, esquemas de seguridad y coincidencia método+ruta con los controladores actuales. En el Gateway contrasta la fachada compuesta y las seis familias declaradas en `GatewayRoutes.java`. Cualquier deriva termina con código distinto de cero.

## Seguridad representada

Las rutas externas protegidas declaran `bearerAuth`, con esquema HTTP Bearer y formato JWT. Las operaciones públicas de login, renovación, cierre de sesión y recuperación de contraseña no declaran JWT. Las APIs internas declaran `internalApiKey`, un encabezado `X-Internal-Api-Key`, en lugar de Bearer. El contrato del Gateway excluye las rutas internas porque no forman parte de la fachada externa.

Los alias `/auth-service/**` y `/usuarios-service/**` continúan en `GatewayRoutes.java`; sus operaciones externas se documentan como obsoletas y de compatibilidad heredada. El Gateway elimina ese primer segmento antes de enviar la solicitud. Las rutas internas de Usuarios están excluidas expresamente del alias por el propio código de routing.

## OpenAPI, Pact y alcance

OpenAPI describe la superficie HTTP, sus parámetros, cuerpos, respuestas y mecanismos de autenticación. Los tests Pact del repositorio verifican interacciones concretas entre consumidores y proveedores. Un contrato no sustituye al otro: OpenAPI ofrece el inventario de interfaz y Pact comprueba escenarios de integración seleccionados.

Los cuatro contratos de servicio incluyen sus APIs externas e internas. El contrato del Gateway representa únicamente rutas que el código de routing expone y reutiliza los schemas de los servicios; no atribuye controladores propios al Gateway.

La extracción estática no ejecuta validaciones de negocio ni demuestra que todos los códigos de error ocurran en producción. Las respuestas documentadas son las derivables de las firmas y construcciones explícitas de los controladores. Los contratos deben regenerarse y validarse cuando cambien controladores, DTO, seguridad o rutas del Gateway.
