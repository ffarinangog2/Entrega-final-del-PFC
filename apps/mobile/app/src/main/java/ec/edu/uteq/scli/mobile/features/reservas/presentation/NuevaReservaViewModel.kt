package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NuevaReservaViewModel(private val repository: ReservaRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NuevaReservaUiState())
    val uiState: StateFlow<NuevaReservaUiState> = mutableUiState.asStateFlow()
    private var idempotencyKey: String? = null

    fun actualizarFormulario(transform: (NuevaReservaUiState) -> NuevaReservaUiState) {
        if (mutableUiState.value.enviando) return
        mutableUiState.value = transform(mutableUiState.value).copy(error = null, solicitudCreada = null)
        idempotencyKey = null
    }

    fun enviar() {
        val state = mutableUiState.value
        if (state.enviando) return
        val participantes = state.numeroParticipantes.toIntOrNull()
        if (camposObligatoriosIncompletos(state) || participantes == null || participantes <= 0) {
            mutableUiState.value = state.copy(error = "campos_invalidos")
            return
        }

        val solicitud = NuevaSolicitudReserva(
            state.solicitanteId.trim(), state.docenteId.trim(), state.laboratorioId.trim(),
            state.materiaId.trim(), state.periodoLectivoId.trim(), state.fechaReserva.trim(),
            state.horaInicio.trim(), state.horaFin.trim(), participantes, state.motivo.trim(),
            state.observacion.trim().ifBlank { null },
        )
        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
        mutableUiState.value = state.copy(enviando = true, error = null)
        viewModelScope.launch {
            when (val disponibilidad = repository.consultarDisponibilidad(
                solicitud.laboratorioId,
                solicitud.fechaReserva,
                solicitud.horaInicio,
                solicitud.horaFin,
            )) {
                is NetworkResult.Failure -> mostrarError(disponibilidad.message)
                is NetworkResult.Success -> {
                    if (!disponibilidad.value.disponible) {
                        mostrarError(disponibilidad.value.motivo ?: "horario_no_disponible")
                    } else {
                        crearSolicitud(solicitud, key)
                    }
                }
            }
        }
    }

    private suspend fun crearSolicitud(solicitud: NuevaSolicitudReserva, key: String) {
        when (val result = repository.crearSolicitud(solicitud, key)) {
            is NetworkResult.Success -> {
                mutableUiState.value = mutableUiState.value.copy(
                    enviando = false,
                    solicitudCreada = result.value,
                )
                idempotencyKey = null
            }
            is NetworkResult.Failure -> mostrarError(result.message)
        }
    }

    private fun mostrarError(message: String) {
        mutableUiState.value = mutableUiState.value.copy(enviando = false, error = message)
    }

    private fun camposObligatoriosIncompletos(state: NuevaReservaUiState): Boolean = listOf(
        state.solicitanteId,
        state.docenteId,
        state.laboratorioId,
        state.materiaId,
        state.periodoLectivoId,
        state.fechaReserva,
        state.horaInicio,
        state.horaFin,
        state.numeroParticipantes,
        state.motivo,
    ).any { it.isBlank() }
}
