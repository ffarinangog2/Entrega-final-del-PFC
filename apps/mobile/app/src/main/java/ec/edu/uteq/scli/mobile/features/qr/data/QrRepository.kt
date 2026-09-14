package ec.edu.uteq.scli.mobile.features.qr.data

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import java.io.IOException

interface QrRepository {
    suspend fun obtenerDetalle(laboratorioId: String): NetworkResult<LaboratorioDetalle>
}

class RemoteQrRepository(
    private val api: QrApi,
) : QrRepository {
    override suspend fun obtenerDetalle(laboratorioId: String): NetworkResult<LaboratorioDetalle> = try {
        val response = api.obtenerDetalle(laboratorioId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            NetworkResult.Success(body.toDomain())
        } else {
            NetworkResult.Failure(response.code(), "gateway_http_${response.code()}")
        }
    } catch (_: IOException) {
        NetworkResult.Failure(null, "gateway_no_disponible")
    } catch (_: RuntimeException) {
        NetworkResult.Failure(null, "respuesta_gateway_invalida")
    }
}
