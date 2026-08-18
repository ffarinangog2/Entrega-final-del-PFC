# Pruebas de carga de Reservas/Solicitudes

Este módulo usa Locust 2.x y solo ejecuta consultas de lectura contra endpoints reales
de `reservas-solicitudes-service`.

## Preparación

```powershell
python -m pip install -r tests/load/requirements.txt
```

El host no está codificado en `locustfile.py`. Debe proporcionarse mediante `--host`
o la variable de entorno nativa `LOCUST_HOST`. Por ejemplo, para una instancia local:

```powershell
$env:LOCUST_HOST = "http://localhost:8084"
```

## Escenario nominal

Usa el `locustfile.py` principal con 50 usuarios durante 5 minutos. Los usuarios se
incorporan a razón de 10 por segundo hasta alcanzar la carga nominal:

```powershell
python -m locust -f tests/load/locustfile.py --headless --users 50 --spawn-rate 10 --run-time 5m
```

## Escenario ramp

`ramp_locustfile.py` reutiliza `ReservasUser` y añade únicamente `RampTo200Users`.
La forma calcula cada segundo el objetivo lineal correspondiente al tiempo transcurrido:
parte de 0, agrega aproximadamente un usuario cada 3 segundos, alcanza cerca de 200
usuarios al final y detiene la ejecución al completar 10 minutos.

```powershell
python -m locust -f tests/load/ramp_locustfile.py --headless
```

También se puede reemplazar `LOCUST_HOST` con `--host http://localhost:8084` en cada
comando. Antes de ejecutar estos escenarios debe comprobarse que el host apunta al
ambiente autorizado y que contiene reservas consultables.

Las métricas se agrupan con los nombres `GET /api/v1/reservas` y
`GET /api/v1/reservas/{id}`. El detalle solo utiliza identificadores UUID descubiertos
en respuestas válidas del listado; si no hay reservas, se continúa consultando el
listado sin fabricar IDs ni crear datos.
