package ec.edu.uteq.scli.mobile.features.notifications

import android.content.Context
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class RegistrarDispositivoRequest(val token: String, val plataforma: String = "ANDROID")
interface DeviceRegistrationApi {
    @POST("api/v1/notificaciones/dispositivos")
    suspend fun registrar(@Body request: RegistrarDispositivoRequest): Response<Unit>
}

class DeviceTokenRegistrar(context: Context, private val api: DeviceRegistrationApi) {
    private val preferences=context.getSharedPreferences("scli_fcm",Context.MODE_PRIVATE)
    fun guardarPendiente(token:String) { preferences.edit().putString(TOKEN_KEY,token).apply() }
    suspend fun registrarPendiente() {
        val token=preferences.getString(TOKEN_KEY,null) ?: return
        runCatching { api.registrar(RegistrarDispositivoRequest(token)) }
            .getOrNull()?.takeIf { it.isSuccessful }?.let { preferences.edit().remove(TOKEN_KEY).apply() }
    }
    private companion object { const val TOKEN_KEY="pending_registration_token" }
}
