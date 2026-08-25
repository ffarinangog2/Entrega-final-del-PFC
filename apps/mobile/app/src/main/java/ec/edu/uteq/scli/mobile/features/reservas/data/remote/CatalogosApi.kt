package ec.edu.uteq.scli.mobile.features.reservas.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

data class DocenteDto(val id: String, val perfilId: String, val codigoDocente: String, val activo: Boolean)
data class MateriaDto(val id: String, val codigo: String, val nombre: String, val activo: Boolean)
data class PeriodoDto(val id: String, val codigo: String, val nombre: String, val activo: Boolean)
data class LaboratorioCatalogoDto(val id: String, val codigo: String, val nombre: String, val pisoId: String?, val activo: Boolean)
data class HorarioDto(val id: String, val docenteId: String, val materiaId: String, val periodoLectivoId: String, val laboratorioId: String?)

interface CatalogosApi {
    @GET("api/v1/docentes/perfil/{perfilId}") suspend fun docentePorPerfil(@Path("perfilId") perfilId: String): DocenteDto
    @GET("api/v1/materias") suspend fun materias(): List<MateriaDto>
    @GET("api/v1/periodos-lectivos/actual") suspend fun periodoActual(): PeriodoDto
    @GET("api/v1/laboratorios") suspend fun laboratorios(): List<LaboratorioCatalogoDto>
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
    suspend fun laboratorios() = api.laboratorios().filter { it.activo }
    suspend fun cargar(perfilId: String): CatalogosSolicitud {
        val docente = api.docentePorPerfil(perfilId)
        return CatalogosSolicitud(docente, api.materias(), api.periodoActual(), api.laboratorios(), api.horarios(docente.id))
    }
}
