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
)

class AuthViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = AuthUiState(restaurando = false, sesion = repository.restoreSession())
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completa usuario y contraseña")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            _uiState.value = when (val result = repository.login(username.trim(), password)) {
                is NetworkResult.Success -> AuthUiState(sesion = result.value)
                is NetworkResult.Failure -> _uiState.value.copy(
                    cargando = false,
                    error = result.message,
                )
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState(restaurando = false)
    }
}
