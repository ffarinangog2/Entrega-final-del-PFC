package ec.edu.uteq.scli.mobile.features.incidentes.domain

import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de incidentes. La única implementación de hoy es
 * local (Room, ver [ec.edu.uteq.scli.mobile.features.incidentes.data.IncidenteLocalRepository]);
 * el ViewModel y la UI dependen solo de esta interfaz, así que el día que se
 * agregue una fuente remota (Retrofit) — o una estrategia local+remota — no
 * hace falta tocarlos.
 */
interface IncidenteRepository {
    fun observarTodos(): Flow<List<Incidente>>
    suspend fun crear(incidente: Incidente): Incidente
    suspend fun refrescar() { }
}
