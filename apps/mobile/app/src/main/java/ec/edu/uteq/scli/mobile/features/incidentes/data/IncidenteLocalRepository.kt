package ec.edu.uteq.scli.mobile.features.incidentes.data

import ec.edu.uteq.scli.mobile.features.incidentes.domain.Incidente
import ec.edu.uteq.scli.mobile.features.incidentes.domain.IncidenteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncidenteLocalRepository(
    private val dao: IncidenteDao,
) : IncidenteRepository {

    override fun observarTodos(): Flow<List<Incidente>> =
        dao.observarTodos().map { entidades -> entidades.map { it.toDomain() } }

    override suspend fun crear(incidente: Incidente): Incidente {
        val id = dao.insertar(incidente.toEntity())
        return incidente.copy(id = id)
    }
}
