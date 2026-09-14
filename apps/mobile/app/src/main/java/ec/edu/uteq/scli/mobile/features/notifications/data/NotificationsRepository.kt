package ec.edu.uteq.scli.mobile.features.notifications.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationsRepository(
    private val api: NotificationsApi,
) {
    private val _pushTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pushTrigger: SharedFlow<Unit> = _pushTrigger.asSharedFlow()

    fun notificarPushRecibido() {
        _pushTrigger.tryEmit(Unit)
    }

    suspend fun listar(): Result<List<NotificacionInternaResponse>> = runCatching {
        api.listar()
    }

    suspend fun contarNoLeidas(): Result<Long> = runCatching {
        api.noLeidas().cantidad
    }

    suspend fun marcarLeida(id: String): Result<NotificacionInternaResponse> = runCatching {
        api.marcarLeida(id)
    }

    suspend fun marcarTodasLeidas(): Result<Unit> = runCatching {
        val response = api.marcarTodasLeidas()
        if (!response.isSuccessful && response.code() != 204) {
            error("Error al marcar todas como leídas: HTTP ${response.code()}")
        }
    }
}
