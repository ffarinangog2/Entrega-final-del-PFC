# Contratos Pact de Reservas (Web, Móvil y Backend)

La fuente de verdad de los contratos está en:
- `WebReservasConsumerPactTest.java` (Consumidor Web: `scli-web`)
- `MobileReservasConsumerPactTest.java` (Consumidor Móvil: `scli-mobile`)
- `ReservasConsumerPactTest.java` (Consumidor base: `scli-contract-tests`)

Su ejecución genera los contratos Pact V4 en `target/pacts/`:
- `scli-web-reservas-solicitudes-service.json`
- `scli-mobile-reservas-solicitudes-service.json`
- `scli-contract-tests-reservas-solicitudes-service.json`

Esta carpeta está ignorada por Git y los JSON se generan y validan deterministamente durante la etapa de pruebas.

Desde la raíz del repositorio, ejecutar:

```powershell
.\tests\contract\verify-reservas-provider.ps1
```

El script aplica este flujo:

1. Ejecuta las interacciones Consumer para `scli-web` y `scli-mobile`;
2. Genera los archivos Pact V4 en `target/pacts/`;
3. Ejecuta `ReservasProviderPactTest` en `reservas-solicitudes-service` contra todos los Pacts generados;
4. Verifica que el backend satisface los contratos de ambos clientes sin diferencias en los DTOs ni rutas.

El Provider usa el controlador real con datos deterministas preparados en
memoria para sus estados. No requiere una base de datos ni un servicio externo.
