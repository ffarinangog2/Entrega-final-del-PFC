package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CatalogosRepository
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NuevaReservaViewModel(
    private val repository: ReservaRepository,
    private val catalogos: CatalogosRepository? = null,
    private val perfilId: String = "",
) : ViewModel() {
    private val state = MutableStateFlow(NuevaReservaUiState())
    val uiState = state.asStateFlow()
    private var idempotencyKey: String? = null

    init { if (catalogos != null) cargarCatalogos() else state.value = state.value.copy(cargandoCatalogos = false) }

    private fun cargarCatalogos() = viewModelScope.launch {
        state.value = try {
            val data = requireNotNull(catalogos).cargar(perfilId)
            val permitidas = data.horarios.map { it.materiaId }.toSet()
            state.value.copy(cargandoCatalogos = false, docenteId = data.docente.id,
                docenteCodigo = data.docente.codigoDocente,
                materias = data.materias.filter { it.activo && (permitidas.isEmpty() || it.id in permitidas) },
                laboratorios = data.laboratorios.filter { it.activo }, periodo = data.periodo)
        } catch (_: Exception) {
            state.value.copy(cargandoCatalogos = false, error = "No fue posible cargar los catálogos")
        }
    }

    fun actualizar(transform: (NuevaReservaUiState) -> NuevaReservaUiState) {
        if (state.value.enviando) return
        state.value = transform(state.value).copy(error = null, disponible = null, solicitudCreada = null)
        idempotencyKey = null
    }

    fun actualizarFormulario(transform: (NuevaReservaUiState) -> NuevaReservaUiState) = actualizar(transform)

    fun comprobarDisponibilidad() {
        val s = state.value
        if (listOf(s.laboratorioId, s.fechaReserva, s.horaInicio, s.horaFin).any(String::isBlank)) return
        state.value = s.copy(comprobando = true, error = null)
        viewModelScope.launch {
            state.value = when (val result = repository.consultarDisponibilidad(s.laboratorioId, s.fechaReserva, s.horaInicio, s.horaFin)) {
                is NetworkResult.Success -> state.value.copy(comprobando = false, disponible = result.value.disponible)
                is NetworkResult.Failure -> state.value.copy(comprobando = false, error = mensaje(result))
            }
        }
    }

    fun enviar() {
        val s = state.value
        if (s.enviando) return
        val participantes = s.numeroParticipantes.toIntOrNull()
        val periodoId = s.periodo?.id ?: s.periodoLectivoId
        if (periodoId.isBlank() || participantes == null || participantes <= 0 ||
            listOf(s.docenteId, s.materiaId, s.laboratorioId, s.fechaReserva, s.horaInicio, s.horaFin, s.motivo).any(String::isBlank)) {
            state.value = s.copy(error = "Completa correctamente todos los campos obligatorios")
            return
        }
        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
        val request = NuevaSolicitudReserva(perfilId.ifBlank { s.solicitanteId }, s.docenteId, s.laboratorioId, s.materiaId,
            periodoId, s.fechaReserva, s.horaInicio, s.horaFin, participantes, s.motivo,
            s.observacion.ifBlank { null })
        state.value = s.copy(enviando = true, error = null)
        viewModelScope.launch {
            state.value = when (val result = repository.crearSolicitud(request, key)) {
                is NetworkResult.Success -> state.value.copy(enviando = false, solicitudCreada = result.value).also { idempotencyKey = null }
                is NetworkResult.Failure -> state.value.copy(enviando = false, error = mensaje(result))
            }
        }
    }

    private fun mensaje(error: NetworkResult.Failure) = when (error.statusCode) {
        401 -> "Tu sesión expiró."
        403 -> "No tienes permisos para realizar esta acción."
        404 -> "No se encontró el recurso."
        409 -> "Existe un conflicto de horario o estado."
        else -> if (error.statusCode == null) "Sin conexión." else "No fue posible procesar la solicitud."
    }
}
