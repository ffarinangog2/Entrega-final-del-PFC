package ec.edu.uteq.scli.mobile.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.features.notifications.data.NotificacionInternaResponse
import ec.edu.uteq.scli.mobile.features.notifications.data.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val loading: Boolean = false,
    val items: List<NotificacionInternaResponse> = emptyList(),
    val cantidadNoLeidas: Long = 0L,
    val error: String? = null,
)

fun badgeCountText(cantidad: Long): String? = when {
    cantidad <= 0L -> null
    cantidad > 99L -> "99+"
    else -> cantidad.toString()
}

class NotificationsViewModel(
    private val repository: NotificationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val itemsResult = repository.listar()
            val countResult = repository.contarNoLeidas()

            itemsResult.fold(
                onSuccess = { items ->
                    val noLeidas = countResult.getOrDefault(items.count { !it.leida }.toLong())
                    _uiState.update {
                        it.copy(
                            loading = false,
                            items = items,
                            cantidadNoLeidas = noLeidas,
                            error = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = err.message ?: "No fue posible cargar las notificaciones",
                        )
                    }
                },
            )
        }
    }

    fun refrescar() = cargar()

    fun cargarNoLeidas() {
        viewModelScope.launch {
            repository.contarNoLeidas().fold(
                onSuccess = { count ->
                    _uiState.update { it.copy(cantidadNoLeidas = count) }
                },
                onFailure = {
                    // Carga silenciosa en fondo; no degrada la UI
                },
            )
        }
    }

    fun marcarLeida(id: String, onExito: () -> Unit = {}) {
        val itemActual = _uiState.value.items.find { it.id == id }
        if (itemActual != null && itemActual.leida) {
            onExito()
            return
        }

        viewModelScope.launch {
            repository.marcarLeida(id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val estabaNoLeida = state.items.find { it.id == id }?.leida == false
                        val nuevosItems = state.items.map { item ->
                            if (item.id == id) item.copy(leida = true) else item
                        }
                        val nuevoConteo = if (estabaNoLeida) {
                            (state.cantidadNoLeidas - 1L).coerceAtLeast(0L)
                        } else {
                            state.cantidadNoLeidas
                        }
                        state.copy(items = nuevosItems, cantidadNoLeidas = nuevoConteo)
                    }
                    onExito()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(error = err.message ?: "No fue posible marcar la notificación como leída")
                    }
                },
            )
        }
    }

    fun marcarTodas() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            repository.marcarTodasLeidas().fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            loading = false,
                            items = state.items.map { it.copy(leida = true) },
                            cantidadNoLeidas = 0L,
                            error = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = err.message ?: "No fue posible marcar todas las notificaciones como leídas",
                        )
                    }
                },
            )
        }
    }
}
