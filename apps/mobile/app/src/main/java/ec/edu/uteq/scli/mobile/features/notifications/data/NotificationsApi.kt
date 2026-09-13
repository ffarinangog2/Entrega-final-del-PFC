package ec.edu.uteq.scli.mobile.features.notifications.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class NotificacionInternaResponse(
    val id: String,
    val titulo: String,
    val cuerpo: String,
    val tipo: String,
    val referenciaId: String? = null,
    val leida: Boolean = false,
    val creadaEn: String = "",
)

data class ConteoNoLeidasResponse(
    val cantidad: Long = 0L,
)

interface NotificationsApi {
    @GET("api/v1/notificaciones")
    suspend fun listar(): List<NotificacionInternaResponse>

    @GET("api/v1/notificaciones/no-leidas")
    suspend fun noLeidas(): ConteoNoLeidasResponse

    @POST("api/v1/notificaciones/{id}/leer")
    suspend fun marcarLeida(@Path("id") id: String): NotificacionInternaResponse

    @POST("api/v1/notificaciones/leer-todas")
    suspend fun marcarTodasLeidas(): Response<Unit>
}
