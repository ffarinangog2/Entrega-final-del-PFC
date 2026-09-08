package ec.edu.uteq.scli.mobile.features.qr.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.institutional.data.InstitutionalRepository
import ec.edu.uteq.scli.mobile.features.qr.data.LaboratorioDetalle
import ec.edu.uteq.scli.mobile.features.qr.data.QrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

data class QrUiState(
    val cargando: Boolean = false,
    val detalle: LaboratorioDetalle? = null,
    val asistenciaRegistrada: Boolean = false,
    val error: QrError? = null,
)

enum class QrError {
    INVALIDO,
    RED,
    SERVICIO,
    REGISTRO_NO_DISPONIBLE,
    YA_REGISTRADO,
    EXPIRADO,
    NO_AUTORIZADO,
    SOLO_ESTUDIANTES,
}

class QrViewModel(
    private val repository: QrRepository,
    private val institutionalRepository: InstitutionalRepository? = null,
    private val esEstudiante: Boolean = true,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()
    private var ultimoQrProcesado: String? = null

    fun procesarQr(valor: String) {
        val normalizado = valor.trim()
        if (normalizado.isBlank() || normalizado == ultimoQrProcesado || _uiState.value.cargando) return
        ultimoQrProcesado = normalizado
        if (normalizado.startsWith("scli-asistencia:")) {
            if (!esEstudiante) {
                _uiState.value = QrUiState(error = QrError.SOLO_ESTUDIANTES)
                return
            }
            registrarAsistencia(normalizado)
            return
        }
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

    private fun registrarAsistencia(valor: String) {
        val partes = valor.split(':', limit = 3)
        if (partes.size != 3 || institutionalRepository == null) {
            _uiState.value = QrUiState(error = QrError.INVALIDO)
            return
        }
        viewModelScope.launch {
            _uiState.value = QrUiState(cargando = true)
            _uiState.value = runCatching { institutionalRepository.registrarAsistencia(partes[1], partes[2]) }
                .fold(
                    onSuccess = { QrUiState(asistenciaRegistrada = true) },
                    onFailure = { throwable ->
                        val code = (throwable as? HttpException)?.code()
                        val error = when (code) {
                            403 -> QrError.NO_AUTORIZADO
                            409 -> QrError.YA_REGISTRADO
                            410 -> QrError.EXPIRADO
                            null -> if (throwable is IOException) QrError.RED else QrError.SERVICIO
                            else -> QrError.REGISTRO_NO_DISPONIBLE
                        }
                        QrUiState(error = error)
                    },
                )
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
