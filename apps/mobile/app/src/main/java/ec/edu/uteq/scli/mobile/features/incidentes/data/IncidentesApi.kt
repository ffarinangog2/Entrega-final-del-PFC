package ec.edu.uteq.scli.mobile.features.incidentes.data

import retrofit2.Response
import retrofit2.http.*

data class CrearIncidenteDto(val laboratorioEquipo: String, val descripcion: String, val prioridad: String, val fecha: String)
data class IncidenteDto(val id: String, val laboratorioEquipo: String, val descripcion: String,
    val prioridad: String, val fecha: String, val estado: String, val creadoEn: String)
data class PaginaIncidentesDto(val contenido: List<IncidenteDto>)
data class ActualizarEstadoIncidenteDto(val estado: String)

interface IncidentesApi {
    @GET("api/v1/incidentes") suspend fun listar(@Query("tamanio") tamanio: Int = 100): Response<PaginaIncidentesDto>
    @POST("api/v1/incidentes") suspend fun crear(@Body request: CrearIncidenteDto): Response<IncidenteDto>
    @GET("api/v1/incidentes/{id}") suspend fun obtener(@Path("id") id: String): Response<IncidenteDto>
    @PATCH("api/v1/incidentes/{id}/estado")
    suspend fun actualizarEstado(
        @Path("id") id: String,
        @Body request: ActualizarEstadoIncidenteDto,
    ): Response<IncidenteDto>
}
