package ec.edu.uteq.scli.mobile.features.institutional.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.features.institutional.data.InstitutionalRepository
import ec.edu.uteq.scli.mobile.features.institutional.data.PlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.RegistroAsistenciaDto
import ec.edu.uteq.scli.mobile.features.institutional.data.SesionAsistenciaDto
import ec.edu.uteq.scli.mobile.features.institutional.data.CoordinacionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InstitutionalUiState(
    val cargando: Boolean = false,
    val planificaciones: List<PlanificacionDto> = emptyList(),
    val coordinacion: CoordinacionData? = null,
    val historial: List<RegistroAsistenciaDto> = emptyList(),
    val sesion: SesionAsistenciaDto? = null,
    val asistentes: List<RegistroAsistenciaDto> = emptyList(),
    val mensaje: String? = null,
    val error: String? = null,
)

class InstitutionalViewModel(private val repository: InstitutionalRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(InstitutionalUiState())
    val uiState = mutableState.asStateFlow()

    fun cargarPlanificaciones() = ejecutar { copy(planificaciones = repository.planificaciones()) }
    fun cargarCoordinacion() = ejecutar {
        val data = repository.coordinacion()
        copy(planificaciones = data.planificaciones, coordinacion = data)
    }
    fun cargarHistorial() = ejecutar { copy(historial = repository.historial()) }
    fun aceptar(id: String) = ejecutar { repository.aceptar(id); copy(mensaje = "Planificación aceptada") }
    fun rechazar(id: String, motivo: String?) = ejecutar { repository.rechazar(id, motivo); copy(mensaje = "Planificación rechazada") }
    fun aceptarPropuesta(id: String) = ejecutar { repository.aceptarPropuesta(id); copy(mensaje = "Propuesta aceptada") }
    fun registrarQr(valor: String) {
        val partes = valor.trim().split(':', limit = 3)
        if (partes.size != 3 || partes[0] != "scli-asistencia") {
            mutableState.value = mutableState.value.copy(error = "El QR no corresponde a una sesión de asistencia")
            return
        }
        ejecutar { repository.registrarAsistencia(partes[1], partes[2]); copy(mensaje = "Asistencia registrada") }
    }

    fun iniciarReserva(id: String, onSuccess: () -> Unit = {}) = ejecutar(onSuccess) {
        repository.iniciarReserva(id)
        copy(mensaje = "Reserva iniciada")
    }
    fun finalizarReserva(id: String, onSuccess: () -> Unit = {}) = ejecutar(onSuccess) {
        repository.finalizarReserva(id)
        copy(mensaje = "Utilización finalizada")
    }
    fun abrirSesion(reservaId: String) = ejecutar { copy(sesion = repository.abrirSesion(reservaId), mensaje = "Sesión abierta") }
    fun refrescarSesion() {
        val id = mutableState.value.sesion?.id ?: return
        ejecutar { copy(sesion = repository.consultarSesion(id), asistentes = repository.asistentes(id)) }
    }
    fun cerrarSesion() {
        val id = mutableState.value.sesion?.id ?: return
        ejecutar {
            repository.cerrarSesion(id)
            copy(sesion = sesion?.copy(estado = "CERRADA", token = null), mensaje = "Sesión cerrada")
        }
    }

    private fun ejecutar(
        onSuccess: () -> Unit = {},
        block: suspend InstitutionalUiState.() -> InstitutionalUiState,
    ) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(cargando = true, error = null, mensaje = null)
            mutableState.value = runCatching { mutableState.value.block() }
                .fold(
                    onSuccess = {
                        onSuccess()
                        it.copy(cargando = false)
                    },
                    onFailure = { mutableState.value.copy(cargando = false, error = "No fue posible completar la operación") },
                )
        }
    }
}
