package ec.edu.uteq.scli.mobile.features.reservas.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReservasApi {
    @GET("api/v1/reservas")
    suspend fun listarReservas(
        @Query("pagina") pagina: Int,
        @Query("tamanio") tamanio: Int,
    ): Response<PaginaDto<ReservaDto>>

    @GET("api/v1/reservas/{id}")
    suspend fun obtenerReserva(@Path("id") id: String): Response<ReservaDto>

    @POST("api/v1/solicitudes")
    suspend fun crearSolicitud(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body solicitud: CrearSolicitudReservaDto,
    ): Response<SolicitudReservaDto>

    @PUT("api/v1/solicitudes/{id}")
    suspend fun actualizarSolicitud(
        @Path("id") id: String,
        @Body solicitud: ActualizarSolicitudReservaDto,
    ): Response<SolicitudReservaDto>

    @POST("api/v1/solicitudes/{id}/cancelar")
    suspend fun cancelarSolicitud(
        @Path("id") id: String,
        @Body request: CancelarSolicitudDto,
    ): Response<SolicitudReservaDto>

    @POST("api/v1/reservas/{id}/cancelar")
    suspend fun cancelarReserva(
        @Path("id") id: String,
        @Body request: CancelarReservaDto,
    ): Response<ReservaDto>

    @GET("api/v1/disponibilidad/laboratorios/{laboratorioId}")
    suspend fun consultarDisponibilidad(
        @Path("laboratorioId") laboratorioId: String,
        @Query("fecha") fecha: String,
        @Query("horaInicio") horaInicio: String,
        @Query("horaFin") horaFin: String,
    ): Response<DisponibilidadDto>
}
