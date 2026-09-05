# ADR-002: Descomposición del SCLI en microservicios

## Estado

Aceptado.

## Contexto

El SCLI integra autenticación, perfiles institucionales, información académica,
inventario de laboratorios y reservas. Estos ámbitos tienen responsabilidades,
ritmos de cambio y necesidades de persistencia diferentes. Concentrarlos en una
sola aplicación aumentaría el acoplamiento entre seguridad, datos personales,
catálogos académicos y reglas de agenda.

El repositorio ya materializa la separación mediante servicios independientes,
imágenes Docker propias, puertos HTTP distintos y bases de datos por servicio en
`docker-compose.yml`. El API Gateway constituye el punto de entrada común para
los clientes Web y móvil.

## Decisión

Adoptar los siguientes límites de servicio:

- `services/auth-service`: autenticación, emisión y rotación de JWT, sesiones de
  refresh, protección de cuentas y recuperación de contraseña.
- `services/usuarios-service`: perfiles institucionales de estudiantes,
  docentes y administradores, datos personales y contexto institucional.
- `services/academico-laboratorios-service`: catálogos académicos, campus,
  bloques, pisos, laboratorios y equipos.
- `services/reservas-solicitudes-service`: solicitudes, reservas confirmadas,
  disponibilidad, bloqueos de agenda y transiciones de estado.
- `services/api-gateway`: fachada de infraestructura para ruteo, CORS y
  validación de autenticación antes de alcanzar los servicios internos.

Cada servicio conserva su código y migraciones. Las llamadas entre servicios se
realizan por contratos HTTP explícitos; el Gateway no contiene dominio de
negocio. Web y móvil consumen el sistema a través del Gateway.

## Consecuencias

### Positivas

- Los límites reflejan responsabilidades de negocio verificables en el código.
- Seguridad, perfiles, catálogos y reservas pueden evolucionar y probarse de
  manera independiente.
- La persistencia por servicio reduce el acoplamiento directo entre esquemas.
- El Gateway ofrece una entrada uniforme sin trasladar reglas empresariales.

### Negativas

- Las operaciones que cruzan límites dependen de disponibilidad de red y deben
  manejar latencia, errores parciales y compatibilidad de contratos.
- La observabilidad, configuración y despliegue requieren coordinación de varios
  procesos e imágenes.
- No existe una transacción ACID única entre bases de datos de servicios; las
  reglas distribuidas necesitan idempotencia y consistencia explícita.

### Neutrales

- Esta decisión documenta la estructura ya implementada. No cambia endpoints,
  lógica, persistencia ni topología de ejecución.
