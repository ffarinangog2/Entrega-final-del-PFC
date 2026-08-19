package ec.edu.uteq.scli.mobile.features.reservas.data

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CancelarReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CancelarSolicitudDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservasApi
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Disponibilidad
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Pagina
import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva
import retrofit2.Response
import java.io.IOException

class RemoteReservaRepository(private val api: ReservasApi) : ReservaRepository {
    override suspend fun listar(pagina: Int, tamanio: Int): NetworkResult<Pagina<Reserva>> =
        request({ api.listarReservas(pagina, tamanio) }) { it.toDomain { dto -> dto.toDomain() } }

    override suspend fun obtener(id: String): NetworkResult<Reserva> =
        request({ api.obtenerReserva(id) }) { it.toDomain() }

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
            NetworkResult.Failure(response.code(), "gateway_http_${response.code()}")
        }
    } catch (_: IOException) {
        NetworkResult.Failure(null, "gateway_no_disponible")
    } catch (_: RuntimeException) {
        NetworkResult.Failure(null, "respuesta_gateway_invalida")
    }
}
