package ec.edu.uteq.scli.mobile.features.incidentes.data

import ec.edu.uteq.scli.mobile.features.incidentes.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.*

class RemoteIncidenteRepository(private val api: IncidentesApi, private val dao: IncidenteDao) : IncidenteRepository {
    override fun observarTodos(): Flow<List<Incidente>> = dao.observarTodos().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refrescar() {
        val response=api.listar(); val body=response.body()
        if (response.isSuccessful && body != null) {
            dao.eliminarTodos(); dao.insertarTodos(body.contenido.map { it.toEntity() })
        }
    }

    override suspend fun crear(incidente: Incidente): Incidente {
        val fecha=Instant.ofEpochMilli(incidente.fechaMillis).atZone(ZoneOffset.UTC).toLocalDate().toString()
        val response=api.crear(CrearIncidenteDto(incidente.laboratorioEquipo,incidente.descripcion,incidente.prioridad.name,fecha))
        val body=response.body()
        if (!response.isSuccessful || body == null) throw IllegalStateException("No se pudo registrar el incidente")
        val entity=body.toEntity(); val localId=dao.insertar(entity)
        return entity.copy(id=localId).toDomain()
    }

    private fun IncidenteDto.toEntity() = IncidenteEntity(remoteId=id, laboratorioEquipo=laboratorioEquipo,
        descripcion=descripcion, prioridad=prioridad,
        fechaMillis=LocalDate.parse(fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        creadoEnMillis=Instant.parse(creadoEn).toEpochMilli(), estado=estado)
}
