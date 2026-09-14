package ec.edu.uteq.scli.mobile.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.auth.data.AuthRepository
import ec.edu.uteq.scli.mobile.features.auth.data.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val restaurando: Boolean = true,
    val cargando: Boolean = false,
    val sesion: AuthSession? = null,
    val error: String? = null,
    val recuperacionCargando: Boolean = false,
    val recuperacionMensaje: String? = null,
    val recuperacionError: String? = null,
)

class AuthViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        repository.onSessionExpired {
            _uiState.value = AuthUiState(restaurando = false, error = "Tu sesión expiró.")
        }
        val restored = repository.restoreSession()
        if (restored != null) {
            _uiState.value = AuthUiState(restaurando = false, sesion = restored)
        } else {
            viewModelScope.launch {
                _uiState.value = AuthUiState(restaurando = false, sesion = repository.refreshSession())
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completa usuario y contraseña")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            _uiState.value = when (val result = repository.login(username.trim(), password)) {
                is NetworkResult.Success -> AuthUiState(restaurando = false, sesion = result.value)
                is NetworkResult.Failure -> _uiState.value.copy(
                    cargando = false,
                    error = result.message,
                )
            }
        }
    }

    fun solicitarRecuperacion(identifier: String) {
        if (identifier.isBlank()) {
            _uiState.value = _uiState.value.copy(recuperacionError = "Ingresa tu usuario o correo institucional")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                recuperacionCargando = true,
                recuperacionError = null,
                recuperacionMensaje = null,
            )
            val result = repository.forgotPassword(identifier.trim())
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        recuperacionCargando = false,
                        recuperacionMensaje = result.value,
                    )
                }
                is NetworkResult.Failure -> {
                    val msg = if (result.message == "error_red") {
                        "No se pudo conectar con el servicio."
                    } else {
                        "El servicio no está disponible en este momento."
                    }
                    _uiState.value = _uiState.value.copy(
                        recuperacionCargando = false,
                        recuperacionError = msg,
                    )
                }
            }
        }
    }

    fun limpiarEstadoRecuperacion() {
        _uiState.value = _uiState.value.copy(
            recuperacionCargando = false,
            recuperacionMensaje = null,
            recuperacionError = null,
        )
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState(restaurando = false)
        }
    }
}
