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

class ReservasViewModel(
    private val repository: ReservaRepository,
    cargarInicialmente: Boolean = true,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReservasUiState())
    val uiState: StateFlow<ReservasUiState> = mutableUiState.asStateFlow()

    init {
        if (cargarInicialmente) cargarReservas()
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
                    error = result.message,
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
}
