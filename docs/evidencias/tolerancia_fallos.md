# Bitácora de tolerancia a fallos

## Objetivo

Verificar el comportamiento del clúster CockroachDB de tres nodos ante la caída de uno y dos nodos, registrando las latencias obtenidas durante cada escenario.

---

## Configuración del entorno

- Base de datos: CockroachDB v26.2.0
- Número de nodos: 3
- Base de datos: reservas_db
- Registros cargados: 100000 solicitudes de reserva

---

## Resultados de latencia

| Escenario                    | Resultado                              |   Latencia |
| ---------------------------- | -------------------------------------- | ---------: |
| Clúster operativo (3 nodos)  | Consulta exitosa                       | 2604.54 ms |
| Un nodo fuera de servicio    | Consulta exitosa                       | 2681.40 ms |
| Clúster recuperado (3 nodos) | Consulta exitosa                       | 2906.34 ms |
| Dos nodos fuera de servicio  | Sin disponibilidad (pérdida de quórum) |  No aplica |

---

## Evidencias

### Figura 1

Clúster con los tres nodos activos.

Archivo:

- cluster_3_nodos_estado.png

### Figura 2

Clúster levantado en Docker.

Archivo:

- cluster_3_nodos_docker.png

### Figura 3

Esquema de tablas de la base de datos.

Archivo:

- schema_tablas_cluster.png

### Figura 4

Configuración de replicación con tres réplicas.

Archivo:

- replicacion_num_replicas_3.png

### Figura 5

Carga correcta de 100000 registros.

Archivo:

- conteo_100000_registros.png

### Figura 6

Prueba con un nodo fuera de servicio.

Archivo:

- tolerancia_fallo_un_nodo.png

### Figura 7

Prueba con dos nodos fuera de servicio.

Archivo:

- tolerancia_fallo_dos_nodos.png

### Figura 8

Recuperación completa del clúster.

Archivo:

- recuperacion_cluster_3_nodos.png

### Figura 9

Latencia antes del fallo.

Archivo:

- latencia_antes_fallo.png

### Figura 10

Latencia con un nodo fuera de servicio.

Archivo:

- latencia_un_nodo_caido.png

### Figura 11

Latencia después de la recuperación.

Archivo:

- latencia_despues_recuperacion.png

---

## Conclusiones

- El clúster permaneció disponible cuando se produjo la caída de un nodo.
- La pérdida simultánea de dos nodos provocó pérdida de quórum y la indisponibilidad temporal del servicio.
- Tras reiniciar los nodos, el clúster recuperó su funcionamiento normal.
- Las mediciones de latencia muestran un incremento moderado durante las pruebas, manteniendo la disponibilidad mientras existió quórum.
