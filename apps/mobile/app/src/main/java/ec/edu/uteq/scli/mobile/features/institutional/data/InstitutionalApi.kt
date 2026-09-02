package ec.edu.uteq.scli.mobile.features.institutional.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PageResponse
import retrofit2.http.Query

data class PlanificacionDto(
    val id: String,
    val periodoId: String,
    val carreraId: String,
    val materiaId: String,
    val docenteId: String?,
    val laboratorioId: String,
    val diaSemana: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: String,
    val observacion: String?,
)

data class MateriaPlanificacionDto(
    val id: String,
    val carreraId: String,
    val codigo: String,
    val nombre: String,
)

data class DocentePlanificacionDto(
    val id: String,
    val codigoDocente: String,
)

data class LaboratorioPlanificacionDto(
    val id: String,
    val codigo: String,
    val nombre: String,
    val estado: String,
)

data class CarreraPlanificacionDto(val id: String, val codigo: String, val nombre: String)
data class PeriodoPlanificacionDto(val id: String, val codigo: String, val nombre: String, val estado: String)

data class CoordinacionData(
    val planificaciones: List<PlanificacionDto>,
    val materias: List<MateriaPlanificacionDto>,
    val docentes: List<DocentePlanificacionDto>,
    val laboratorios: List<LaboratorioPlanificacionDto>,
    val carreras: List<CarreraPlanificacionDto>,
    val periodo: PeriodoPlanificacionDto,
)

data class ObservacionRequest(val observacion: String?)
data class PropuestaPlanificacionRequest(
    val laboratorioId: String? = null,
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val observacion: String,
)
data class RegistroAsistenciaDto(
    val id: String,
    val sesionId: String,
    val estudianteId: String,
    val registradaEn: String,
    val estado: String,
)
data class RegistrarAsistenciaRequest(val token: String)
data class AbrirSesionAsistenciaRequest(val reservaId: String)
data class SesionAsistenciaDto(
    val id: String,
    val reservaId: String,
    val abiertaEn: String,
    val expiraEn: String,
    val estado: String,
    val token: String?,
)

interface InstitutionalApi {
    @GET("api/v1/planificaciones")
    suspend fun listarPlanificaciones(): List<PlanificacionDto>

    @GET("api/v1/materias")
    suspend fun listarMaterias(@Query("page") page: Int, @Query("size") size: Int): PageResponse<MateriaPlanificacionDto>

    @GET("api/v1/laboratorios")
    suspend fun listarLaboratorios(@Query("page") page: Int, @Query("size") size: Int): PageResponse<LaboratorioPlanificacionDto>

    @GET("api/v1/carreras")
    suspend fun listarCarreras(@Query("page") page: Int, @Query("size") size: Int): PageResponse<CarreraPlanificacionDto>

    @GET("api/v1/periodos-lectivos/actual")
    suspend fun periodoActual(): PeriodoPlanificacionDto

    @POST("api/v1/planificaciones/{id}/aceptar")
    suspend fun aceptarPlanificacion(@Path("id") id: String): PlanificacionDto

    @POST("api/v1/planificaciones/{id}/rechazar")
    suspend fun rechazarPlanificacion(
        @Path("id") id: String,
        @Body request: ObservacionRequest,
    ): PlanificacionDto

    @POST("api/v1/planificaciones/{id}/proponer-alternativa")
    suspend fun proponerPlanificacion(
        @Path("id") id: String,
        @Body request: PropuestaPlanificacionRequest,
    ): PlanificacionDto

    @POST("api/v1/planificaciones/{id}/aceptar-propuesta")
    suspend fun aceptarPropuesta(@Path("id") id: String): PlanificacionDto

    @POST("api/v1/asistencias/sesiones/{id}/registros")
    suspend fun registrarAsistencia(
        @Path("id") sesionId: String,
        @Body request: RegistrarAsistenciaRequest,
    ): RegistroAsistenciaDto

    @GET("api/v1/asistencias/historial")
    suspend fun historialAsistencia(): List<RegistroAsistenciaDto>

    @POST("api/v1/reservas/{id}/iniciar")
    suspend fun iniciarReserva(@Path("id") id: String): ReservaDto

    @POST("api/v1/reservas/{id}/finalizar")
    suspend fun finalizarReserva(@Path("id") id: String): ReservaDto

    @POST("api/v1/asistencias/sesiones")
    suspend fun abrirSesion(@Body request: AbrirSesionAsistenciaRequest): SesionAsistenciaDto

    @GET("api/v1/asistencias/sesiones/{id}")
    suspend fun consultarSesion(@Path("id") id: String): SesionAsistenciaDto

    @GET("api/v1/asistencias/sesiones/{id}/registros")
    suspend fun listarAsistentes(@Path("id") id: String): List<RegistroAsistenciaDto>

    @POST("api/v1/asistencias/sesiones/{id}/cerrar")
    suspend fun cerrarSesion(@Path("id") id: String): Response<Unit>
}

class InstitutionalRepository(private val api: InstitutionalApi) {
    suspend fun planificaciones() = api.listarPlanificaciones()
    suspend fun coordinacion() = CoordinacionData(
        planificaciones = api.listarPlanificaciones(),
        materias = todasLasPaginas(api::listarMaterias),
        // Coordinación no consulta el catálogo global de usuarios, que requiere USUARIO_LEER.
        docentes = emptyList(),
        laboratorios = todasLasPaginas(api::listarLaboratorios),
        carreras = todasLasPaginas(api::listarCarreras),
        periodo = api.periodoActual(),
    )
    suspend fun aceptar(id: String) = api.aceptarPlanificacion(id)
    suspend fun rechazar(id: String, motivo: String?) = api.rechazarPlanificacion(id, ObservacionRequest(motivo))
    suspend fun proponer(id: String, observacion: String) =
        api.proponerPlanificacion(id, PropuestaPlanificacionRequest(observacion = observacion))
    suspend fun aceptarPropuesta(id: String) = api.aceptarPropuesta(id)
    suspend fun registrarAsistencia(sesionId: String, token: String) =
        api.registrarAsistencia(sesionId, RegistrarAsistenciaRequest(token))
    suspend fun historial() = api.historialAsistencia()
    suspend fun iniciarReserva(id: String) = api.iniciarReserva(id)
    suspend fun finalizarReserva(id: String) = api.finalizarReserva(id)
    suspend fun abrirSesion(reservaId: String) = api.abrirSesion(AbrirSesionAsistenciaRequest(reservaId))
    suspend fun consultarSesion(id: String) = api.consultarSesion(id)
    suspend fun asistentes(id: String) = api.listarAsistentes(id)
    suspend fun cerrarSesion(id: String) {
        val response = api.cerrarSesion(id)
        if (!response.isSuccessful) error("No fue posible cerrar la sesión")
    }

    private suspend fun <T> todasLasPaginas(
        cargar: suspend (page: Int, size: Int) -> PageResponse<T>,
    ): List<T> {
        val resultado = mutableListOf<T>()
        var pagina = 0
        do {
            val respuesta = cargar(pagina, 100)
            resultado += respuesta.content
            pagina++
        } while (!respuesta.last && pagina < respuesta.totalPages)
        return resultado
    }
}
