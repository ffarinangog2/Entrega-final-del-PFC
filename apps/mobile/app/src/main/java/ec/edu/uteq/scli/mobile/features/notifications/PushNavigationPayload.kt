package ec.edu.uteq.scli.mobile.features.notifications

import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

const val EXTRA_PUSH_TIPO = "push_tipo"
const val EXTRA_PUSH_REFERENCIA_ID = "push_referencia_id"

data class PushNavigationPayload(
    val tipo: String,
    val referenciaId: String? = null,
)

fun parsePushNavigationPayload(data: Map<String, String>?): PushNavigationPayload? {
    if (data == null) return null
    val tipo = (data[EXTRA_PUSH_TIPO] ?: data["tipo"])?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val referenciaId = (data[EXTRA_PUSH_REFERENCIA_ID] ?: data["referenciaId"])?.trim()?.takeIf { it.isNotEmpty() }
        ?: data["solicitudId"]?.trim()?.takeIf { it.isNotEmpty() }
        ?: data["planificacionId"]?.trim()?.takeIf { it.isNotEmpty() }
        ?: data["incidenteId"]?.trim()?.takeIf { it.isNotEmpty() }
    return PushNavigationPayload(tipo = tipo, referenciaId = referenciaId)
}

fun parsePushNavigationPayload(bundle: Bundle?): PushNavigationPayload? {
    if (bundle == null) return null
    val map = mutableMapOf<String, String>()
    for (key in bundle.keySet()) {
        val value = bundle.getString(key)
        if (value != null) {
            map[key] = value
        }
    }
    return parsePushNavigationPayload(map)
}

fun parsePushNavigationPayload(intent: Intent?): PushNavigationPayload? {
    return parsePushNavigationPayload(intent?.extras)
}

fun calcularPushRequestCode(payload: PushNavigationPayload?): Int {
    if (payload == null) return 0
    val hash = 31 * payload.tipo.hashCode() + (payload.referenciaId?.hashCode() ?: 0)
    val positive = abs(hash)
    return if (positive == 0) 1 else positive
}

class PushNavigationManager {
    private val _pendingPayload = MutableStateFlow<PushNavigationPayload?>(null)
    val pendingPayload: StateFlow<PushNavigationPayload?> = _pendingPayload.asStateFlow()

    fun emitPayload(payload: PushNavigationPayload) {
        _pendingPayload.value = payload
    }

    fun consumePayload(): PushNavigationPayload? {
        val current = _pendingPayload.value
        _pendingPayload.value = null
        return current
    }

    fun clear() {
        _pendingPayload.value = null
    }
}
