package ec.edu.uteq.scli.mobile.features.qr.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface QrApi {
    @GET("api/v1/laboratorios/{id}/detalle-completo")
    suspend fun obtenerDetalle(@Path("id") laboratorioId: String): Response<LaboratorioDetalleDto>
}
