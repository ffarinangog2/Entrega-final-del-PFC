# Contratos Pact de Reservas

La fuente de verdad es `ReservasConsumerPactTest.java`. Su ejecución genera el
Pact V4 en `target/pacts/`; esta carpeta está ignorada por Git y el JSON no debe
editarse ni copiarse manualmente.

Desde la raíz del repositorio, ejecutar:

```powershell
.\tests\contract\verify-reservas-provider.ps1
```

El script aplica este flujo:

1. ejecuta las dos interacciones Consumer;
2. genera `target/pacts/scli-contract-tests-reservas-solicitudes-service.json`;
3. ejecuta `ReservasProviderPactTest` contra ese archivo recién generado;
4. verifica las dos interacciones Provider.

El Provider usa el controlador real con datos deterministas preparados en
memoria para sus estados. No requiere una base de datos ni un servicio externo.
