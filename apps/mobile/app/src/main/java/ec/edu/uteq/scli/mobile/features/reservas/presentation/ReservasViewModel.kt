package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.common.network.DataSource
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ec.edu.uteq.scli.mobile.features.reservas.domain.PropuestaAlternativa
import java.util.UUID

class ReservasViewModel(
    private val repository: ReservaRepository,
    cargarInicialmente: Boolean = true,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReservasUiState())
    val uiState: StateFlow<ReservasUiState> = mutableUiState.asStateFlow()

    init {
        if (cargarInicialmente) cargarTodo()
    }

    fun cargarTodo() { cargarReservas(); cargarSolicitudes() }

    fun cargarSolicitudes() = viewModelScope.launch {
        when (val result = repository.listarSolicitudes()) {
            is NetworkResult.Success -> mutableUiState.value = mutableUiState.value.copy(solicitudes = result.value.contenido)
            is NetworkResult.Failure -> if (result.message != "no_implementado") {
                mutableUiState.value = mutableUiState.value.copy(error = mensaje(result))
            }
        }
    }

    fun cargarSolicitud(id: String) = viewModelScope.launch {
        mutableUiState.value = mutableUiState.value.copy(cargando = true, error = null)
        val solicitud = repository.obtenerSolicitud(id)
        val historial = repository.historial(id)
        mutableUiState.value = if (solicitud is NetworkResult.Success) mutableUiState.value.copy(
            cargando = false, solicitudSeleccionada = solicitud.value,
            historial = (historial as? NetworkResult.Success)?.value?.contenido.orEmpty())
        else mutableUiState.value.copy(cargando = false, error = mensaje(solicitud as NetworkResult.Failure))
    }

    fun actuarSolicitud(action: String, perfilId: String, comentario: String = "", propuesta: PropuestaAlternativa? = null) = viewModelScope.launch {
        val actual = mutableUiState.value.solicitudSeleccionada ?: return@launch
        val result = when (action) {
            "revision" -> repository.ponerEnRevision(actual.id)
            "rechazar" -> repository.rechazar(actual.id, comentario)
            "cancelar" -> repository.cancelarSolicitud(actual.id, comentario)
            "aceptar" -> repository.responderPropuesta(actual.id, true, comentario)
            "rechazar_propuesta" -> repository.responderPropuesta(actual.id, false, comentario)
            "proponer" -> repository.proponer(actual.id, requireNotNull(propuesta))
            "aprobar" -> {
                when (val approved = repository.aprobar(actual.id, perfilId, comentario, UUID.randomUUID().toString())) {
                    is NetworkResult.Success -> { cargarSolicitud(actual.id); return@launch }
                    is NetworkResult.Failure -> approved
                }
            }
            else -> return@launch
        }
        mutableUiState.value = when (result) {
            is NetworkResult.Success -> mutableUiState.value.copy(solicitudSeleccionada = result.value, error = null)
            is NetworkResult.Failure -> mutableUiState.value.copy(error = mensaje(result))
        }
    }

    fun cargarReservas(esRefresco: Boolean = false) {
        mutableUiState.value = mutableUiState.value.copy(
            cargando = !esRefresco,
            refrescando = esRefresco,
            error = null,
        )
        viewModelScope.launch {
            when (val result = repository.listar()) {
                is NetworkResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    reservas = result.value.contenido,
                    cargando = false,
                    refrescando = false,
                    desdeCache = result.source == DataSource.CACHE,
                    errorActualizacion = result.refreshError,
                )
                is NetworkResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    cargando = false,
                    refrescando = false,
                    error = mensaje(result),
                )
            }
        }
    }

    fun cargarDetalle(id: String) {
        mutableUiState.value = mutableUiState.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val result = repository.obtener(id)) {
                is NetworkResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    seleccionada = result.value,
                    cargando = false,
                    error = null,
                    desdeCache = result.source == DataSource.CACHE,
                    errorActualizacion = result.refreshError,
                )
                is NetworkResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    cargando = false,
                    error = result.message,
                )
            }
        }
    }

    fun cancelarReserva(motivo: String) {
        val reserva = mutableUiState.value.seleccionada ?: return
        if (reserva.estado != "PROGRAMADA" || motivo.isBlank() || mutableUiState.value.cancelando) return
        mutableUiState.value = mutableUiState.value.copy(cancelando = true, error = null)
        viewModelScope.launch {
            when (val result = repository.cancelarReserva(reserva.id, motivo.trim())) {
                is NetworkResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    seleccionada = result.value,
                    reservas = mutableUiState.value.reservas.map {
                        if (it.id == result.value.id) result.value else it
                    },
                    cancelando = false,
                    cancelacionExitosa = true,
                )
                is NetworkResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    cancelando = false,
                    error = result.message,
                )
            }
        }
    }

    fun consumirCancelacionExitosa() {
        mutableUiState.value = mutableUiState.value.copy(cancelacionExitosa = false)
    }

    private fun mensaje(error: NetworkResult.Failure) = when (error.statusCode) {
        401 -> "Tu sesión expiró."
        403 -> "No tienes permisos para realizar esta acción."
        404 -> "No se encontró el recurso."
        409 -> "Existe un conflicto de horario o estado."
        else -> if (error.statusCode == null) "Sin conexión." else "No fue posible procesar la solicitud."
    }
}
