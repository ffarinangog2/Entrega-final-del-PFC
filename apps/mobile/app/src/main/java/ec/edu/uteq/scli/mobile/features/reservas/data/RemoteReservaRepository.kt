package ec.edu.uteq.scli.mobile.features.reservas.data

import com.google.gson.JsonParser
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.common.network.DataSource
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CancelarReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CancelarSolicitudDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservasApi
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaDao
import ec.edu.uteq.scli.mobile.features.reservas.data.local.toDomain
import ec.edu.uteq.scli.mobile.features.reservas.data.local.toEntity
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Disponibilidad
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Pagina
import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.HistorialSolicitud
import ec.edu.uteq.scli.mobile.features.reservas.domain.PropuestaAlternativa
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.AprobarSolicitudDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ComentarioDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PropuestaDto
import retrofit2.Response
import java.io.IOException

class RemoteReservaRepository(
    private val api: ReservasApi,
    private val reservaDao: ReservaDao,
) : ReservaRepository {
    override suspend fun listarSolicitudes(): NetworkResult<Pagina<SolicitudReserva>> =
        request({ api.listarSolicitudes() }) { it.toDomain { dto -> dto.toDomain() } }

    override suspend fun obtenerSolicitud(id: String): NetworkResult<SolicitudReserva> =
        request({ api.obtenerSolicitud(id) }) { it.toDomain() }

    override suspend fun historial(id: String): NetworkResult<Pagina<HistorialSolicitud>> =
        request({ api.historial(id) }) { it.toDomain { dto -> dto.toDomain() } }
    override suspend fun listar(pagina: Int, tamanio: Int): NetworkResult<Pagina<Reserva>> = try {
        val response = api.listarReservas(pagina, tamanio)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            val page = body.toDomain { dto -> dto.toDomain() }
            reservaDao.guardarTodas(page.contenido.map { it.toEntity() })
            NetworkResult.Success(page)
        } else {
            NetworkResult.Failure(response.code(), "gateway_http_${response.code()}")
        }
    } catch (_: IOException) {
        val cached = runCatching { reservaDao.obtenerTodas().map { it.toDomain() } }.getOrDefault(emptyList())
        if (cached.isEmpty()) NetworkResult.Failure(null, "gateway_no_disponible")
        else NetworkResult.Success(
            Pagina(cached, 0, cached.size, cached.size.toLong(), 1, primera = true, ultima = true),
            source = DataSource.CACHE,
            refreshError = "gateway_no_disponible",
        )
    } catch (_: RuntimeException) {
        NetworkResult.Failure(null, "respuesta_gateway_invalida")
    }

    override suspend fun obtener(id: String): NetworkResult<Reserva> = try {
        val response = api.obtenerReserva(id)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            val reserva = body.toDomain()
            reservaDao.guardar(reserva.toEntity())
            NetworkResult.Success(reserva)
        } else {
            NetworkResult.Failure(response.code(), "gateway_http_${response.code()}")
        }
    } catch (_: IOException) {
        val cached = runCatching { reservaDao.obtenerPorId(id)?.toDomain() }.getOrNull()
        if (cached == null) NetworkResult.Failure(null, "gateway_no_disponible")
        else NetworkResult.Success(cached, DataSource.CACHE, "gateway_no_disponible")
    } catch (_: RuntimeException) {
        NetworkResult.Failure(null, "respuesta_gateway_invalida")
    }

    override suspend fun crearSolicitud(
        solicitud: NuevaSolicitudReserva,
        idempotencyKey: String,
    ): NetworkResult<SolicitudReserva> =
        request({ api.crearSolicitud(idempotencyKey, solicitud.toDto()) }) { it.toDomain() }

    override suspend fun actualizarSolicitud(
        id: String,
        solicitud: ActualizacionSolicitudReserva,
    ): NetworkResult<SolicitudReserva> =
        request({ api.actualizarSolicitud(id, solicitud.toDto()) }) { it.toDomain() }

    override suspend fun cancelarSolicitud(id: String, comentario: String): NetworkResult<SolicitudReserva> =
        request({ api.cancelarSolicitud(id, CancelarSolicitudDto(comentario)) }) { it.toDomain() }

    override suspend fun cancelarReserva(id: String, motivo: String): NetworkResult<Reserva> =
        request({ api.cancelarReserva(id, CancelarReservaDto(motivo)) }) { it.toDomain() }

    override suspend fun ponerEnRevision(id: String): NetworkResult<SolicitudReserva> =
        request({ api.revision(id) }) { it.toDomain() }

    override suspend fun aprobar(id: String, responsableId: String, comentario: String?, key: String): NetworkResult<Reserva> =
        request({ api.aprobar(id, key, AprobarSolicitudDto(responsableId, comentario)) }) { it.toDomain() }

    override suspend fun rechazar(id: String, comentario: String): NetworkResult<SolicitudReserva> =
        request({ api.rechazar(id, ComentarioDto(comentario)) }) { it.toDomain() }

    override suspend fun proponer(id: String, propuesta: PropuestaAlternativa): NetworkResult<SolicitudReserva> =
        request({ api.proponer(id, PropuestaDto(propuesta.laboratorioId, propuesta.fecha, propuesta.horaInicio, propuesta.horaFin, propuesta.observacion)) }) { it.toDomain() }

    override suspend fun responderPropuesta(id: String, aceptar: Boolean, comentario: String?): NetworkResult<SolicitudReserva> =
        request({ if (aceptar) api.aceptarPropuesta(id, ComentarioDto(comentario)) else api.rechazarPropuesta(id, ComentarioDto(comentario)) }) { it.toDomain() }

    override suspend fun consultarDisponibilidad(
        laboratorioId: String,
        fecha: String,
        horaInicio: String,
        horaFin: String,
    ): NetworkResult<Disponibilidad> = request(
        { api.consultarDisponibilidad(laboratorioId, fecha, horaInicio, horaFin) },
    ) { it.toDomain() }

    private suspend fun <Dto, Domain> request(
        call: suspend () -> Response<Dto>,
        mapper: (Dto) -> Domain,
    ): NetworkResult<Domain> = try {
        val response = call()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            NetworkResult.Success(mapper(body))
        } else {
            val code = response.code()
            val message = if (code == 400) {
                extraerMensajeApiError(runCatching { response.errorBody()?.string() }.getOrNull())
                    ?: "gateway_http_400"
            } else {
                "gateway_http_$code"
            }
            NetworkResult.Failure(code, message)
        }
    } catch (_: IOException) {
        NetworkResult.Failure(null, "gateway_no_disponible")
    } catch (_: RuntimeException) {
        NetworkResult.Failure(null, "respuesta_gateway_invalida")
    }
}

internal fun extraerMensajeApiError(cuerpo: String?): String? {
    if (cuerpo.isNullOrBlank()) return null
    return try {
        val elemento = JsonParser.parseString(cuerpo)
        if (!elemento.isJsonObject) return null
        val obj = elemento.asJsonObject
        if (!obj.has("message") || obj.get("message").isJsonNull) return null
        val mensaje = obj.get("message").asString.trim()
        if (mensaje.isEmpty() || esTextoTecnico(mensaje)) null else mensaje
    } catch (_: Exception) {
        null
    }
}

private fun esTextoTecnico(texto: String): Boolean {
    val lower = texto.lowercase()
    return lower.contains("<html") ||
        lower.contains("<!doctype") ||
        lower.contains("exception:") ||
        lower.contains("traceback") ||
        (lower.contains("select ") && lower.contains(" from ")) ||
        lower.contains("insert into ") ||
        (lower.contains("update ") && lower.contains(" set ")) ||
        lower.contains("delete from ") ||
        (texto.contains("at ") && texto.contains(".java:")) ||
        texto.length > 300
}
