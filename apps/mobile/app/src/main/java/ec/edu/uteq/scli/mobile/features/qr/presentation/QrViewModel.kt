package ec.edu.uteq.scli.mobile.features.qr.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.qr.data.LaboratorioDetalle
import ec.edu.uteq.scli.mobile.features.qr.data.QrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

 data class QrUiState(
    val cargando: Boolean = false,
    val detalle: LaboratorioDetalle? = null,
    val error: QrError? = null,
)

enum class QrError {
    INVALIDO,
    RED,
    SERVICIO,
}

class QrViewModel(
    private val repository: QrRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()
    private var ultimoQrProcesado: String? = null

    fun procesarQr(valor: String) {
        val normalizado = valor.trim()
        if (normalizado.isBlank() || normalizado == ultimoQrProcesado || _uiState.value.cargando) return
        ultimoQrProcesado = normalizado
        val laboratorioId = extraerUuid(normalizado)
        if (laboratorioId == null) {
            _uiState.value = QrUiState(error = QrError.INVALIDO)
            return
        }
        viewModelScope.launch {
            _uiState.value = QrUiState(cargando = true)
            _uiState.value = when (val result = repository.obtenerDetalle(laboratorioId.toString())) {
                is NetworkResult.Success -> QrUiState(detalle = result.value)
                is NetworkResult.Failure -> QrUiState(error = if (result.statusCode == null) QrError.RED else QrError.SERVICIO)
            }
        }
    }

    fun reintentar() {
        ultimoQrProcesado = null
        _uiState.value = QrUiState()
    }

    companion object {
        fun extraerUuid(valor: String): UUID? {
            val candidato = valor.trim().removeSuffix("/").substringAfterLast('/').substringBefore('?').substringBefore('#')
            return runCatching { UUID.fromString(candidato) }.getOrNull()
        }
    }
}
