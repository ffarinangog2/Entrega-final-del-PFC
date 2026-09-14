package ec.edu.uteq.scli.mobile.features.notifications

import android.content.Context
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

data class RegistrarDispositivoRequest(val token: String, val plataforma: String = "ANDROID")
data class DesregistrarDispositivoRequest(val token: String)
interface DeviceRegistrationApi {
    @POST("api/v1/notificaciones/dispositivos")
    suspend fun registrar(@Body request: RegistrarDispositivoRequest): Response<Unit>
    @HTTP(method = "DELETE", path = "api/v1/notificaciones/dispositivos", hasBody = true)
    suspend fun desregistrar(@Body request: DesregistrarDispositivoRequest): Response<Unit>
}

class DeviceTokenRegistrar(context: Context, private val api: DeviceRegistrationApi) {
    private val preferences=context.getSharedPreferences("scli_fcm",Context.MODE_PRIVATE)
    fun guardarPendiente(token:String) { preferences.edit().putString(TOKEN_KEY,token).apply() }
    suspend fun registrarPendiente() {
        procesarBajaPendiente()
        val token=preferences.getString(TOKEN_KEY,null) ?: return
        runCatching { api.registrar(RegistrarDispositivoRequest(token)) }
            .getOrNull()?.takeIf { it.isSuccessful }?.let {
                preferences.edit().putString(REGISTERED_TOKEN_KEY, token).apply()
            }
    }
    suspend fun desregistrarActual() {
        val token = preferences.getString(REGISTERED_TOKEN_KEY, null)
            ?: preferences.getString(TOKEN_KEY, null)
            ?: return
        val response = runCatching { api.desregistrar(DesregistrarDispositivoRequest(token)) }.getOrNull()
        if (response?.isSuccessful == true) {
            preferences.edit().remove(REGISTERED_TOKEN_KEY).remove(PENDING_UNREGISTER_KEY).apply()
        } else {
            preferences.edit().putString(PENDING_UNREGISTER_KEY, token).apply()
        }
    }
    private suspend fun procesarBajaPendiente() {
        val token = preferences.getString(PENDING_UNREGISTER_KEY, null) ?: return
        val response = runCatching { api.desregistrar(DesregistrarDispositivoRequest(token)) }.getOrNull()
        if (response?.isSuccessful == true) preferences.edit().remove(PENDING_UNREGISTER_KEY).remove(REGISTERED_TOKEN_KEY).apply()
    }
    private companion object {
        const val TOKEN_KEY="current_fcm_token"
        const val REGISTERED_TOKEN_KEY="registered_token"
        const val PENDING_UNREGISTER_KEY="pending_unregistration_token"
    }
}
