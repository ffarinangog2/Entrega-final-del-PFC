package ec.edu.uteq.scli.mobile.features.incidentes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.features.incidentes.domain.Incidente
import ec.edu.uteq.scli.mobile.features.incidentes.domain.IncidenteRepository
import ec.edu.uteq.scli.mobile.features.incidentes.domain.Prioridad
import ec.edu.uteq.scli.mobile.features.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class IncidentesViewModel(
    private val repository: IncidenteRepository,
    private val notificationHelper: NotificationHelper,
) : ViewModel() {

    init { viewModelScope.launch { runCatching { repository.refrescar() } } }

    private data class FormState(
        val laboratorioEquipo: String = "",
        val descripcion: String = "",
        val prioridad: Prioridad = Prioridad.MEDIA,
        val fechaMillis: Long = System.currentTimeMillis(),
        val error: String? = null,
    )

    private val formState = MutableStateFlow(FormState())

    val uiState: StateFlow<IncidentesUiState> = combine(
        repository.observarTodos(),
        formState,
    ) { incidentes, form ->
        IncidentesUiState(
            incidentes = incidentes,
            laboratorioEquipo = form.laboratorioEquipo,
            descripcion = form.descripcion,
            prioridad = form.prioridad,
            fechaMillis = form.fechaMillis,
            error = form.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IncidentesUiState(),
    )

    fun onLaboratorioEquipoChange(valor: String) {
        formState.value = formState.value.copy(laboratorioEquipo = valor, error = null)
    }

    fun onDescripcionChange(valor: String) {
        formState.value = formState.value.copy(descripcion = valor, error = null)
    }

    fun onPrioridadChange(valor: Prioridad) {
        formState.value = formState.value.copy(prioridad = valor)
    }

    fun onFechaChange(millis: Long) {
        formState.value = formState.value.copy(fechaMillis = millis)
    }

    fun onGuardarIncidente() {
        val form = formState.value

        if (form.laboratorioEquipo.isBlank() || form.descripcion.isBlank()) {
            formState.value = form.copy(error = "campos_incompletos")
            return
        }

        viewModelScope.launch {
            val incidente = Incidente(
                laboratorioEquipo = form.laboratorioEquipo.trim(),
                descripcion = form.descripcion.trim(),
                prioridad = form.prioridad,
                fechaMillis = form.fechaMillis,
            )

            val creado = try { repository.crear(incidente) } catch (_: RuntimeException) {
                formState.value = form.copy(error = "servicio_no_disponible")
                return@launch
            }
            Timber.tag("IncidentesViewModel").i("Incidente creado id=%s prioridad=%s", creado.id, creado.prioridad)

            notificationHelper.mostrar(
                titulo = "Incidente reportado",
                cuerpo = creado.laboratorioEquipo,
            )

            formState.value = FormState()
        }
    }
}
