package ec.edu.uteq.scli.mobile.features.reservas.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class DocenteDto(val id: String, val perfilId: String, val codigoDocente: String, val activo: Boolean)
data class MateriaDto(val id: String, val codigo: String, val nombre: String, val activo: Boolean)
enum class EstadoPeriodoDto { PLANIFICADO, ACTIVO, FINALIZADO }
data class PeriodoDto(val id: String, val codigo: String, val nombre: String, val estado: EstadoPeriodoDto)
data class LaboratorioCatalogoDto(val id: String, val codigo: String, val nombre: String, val pisoId: String?, val activo: Boolean)
data class HorarioDto(val id: String, val docenteId: String, val materiaId: String, val periodoLectivoId: String, val laboratorioId: String?)

/** Contrato real de Spring Data Page que exponen los catálogos académicos. */
data class PageResponse<T>(
    val content: List<T>,
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val numberOfElements: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean,
)

interface CatalogosApi {
    @GET("api/v1/docentes/perfil/{perfilId}") suspend fun docentePorPerfil(@Path("perfilId") perfilId: String): DocenteDto
    @GET("api/v1/materias") suspend fun materias(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageResponse<MateriaDto>
    @GET("api/v1/periodos-lectivos/actual") suspend fun periodoActual(): PeriodoDto
    @GET("api/v1/laboratorios") suspend fun laboratorios(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageResponse<LaboratorioCatalogoDto>
    @GET("api/v1/horarios/docente/{docenteId}") suspend fun horarios(@Path("docenteId") docenteId: String): List<HorarioDto>
}

data class CatalogosSolicitud(
    val docente: DocenteDto,
    val materias: List<MateriaDto>,
    val periodo: PeriodoDto,
    val laboratorios: List<LaboratorioCatalogoDto>,
    val horarios: List<HorarioDto>,
)

class CatalogosRepository(private val api: CatalogosApi) {
    suspend fun laboratorios() = todasLasPaginas(api::laboratorios).filter { it.activo }
    suspend fun cargar(perfilId: String): CatalogosSolicitud {
        val docente = api.docentePorPerfil(perfilId)
        return CatalogosSolicitud(
            docente,
            todasLasPaginas(api::materias),
            api.periodoActual(),
            todasLasPaginas(api::laboratorios),
            api.horarios(docente.id),
        )
    }

    private suspend fun <T> todasLasPaginas(
        cargar: suspend (page: Int, size: Int) -> PageResponse<T>,
    ): List<T> {
        val resultado = mutableListOf<T>()
        var pagina = 0
        do {
            val respuesta = cargar(pagina, PAGE_SIZE)
            resultado += respuesta.content
            pagina++
        } while (!respuesta.last && pagina < respuesta.totalPages)
        return resultado
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}
