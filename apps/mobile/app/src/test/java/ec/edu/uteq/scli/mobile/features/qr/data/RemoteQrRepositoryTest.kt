package ec.edu.uteq.scli.mobile.features.qr.data

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RemoteQrRepositoryTest {
    private val api = mockk<QrApi>()
    private val repository = RemoteQrRepository(api)

    @Test
    fun `detalle exitoso mapea el dto al dominio`() = runTest {
        coEvery { api.obtenerDetalle("laboratorio-1") } returns Response.success(DTO)

        val result = repository.obtenerDetalle("laboratorio-1")

        assertTrue(result is NetworkResult.Success)
        assertEquals("laboratorio-1", (result as NetworkResult.Success).value.laboratorio.id)
    }

    @Test
    fun `respuesta http de error mapea el codigo`() = runTest {
        coEvery { api.obtenerDetalle(any()) } returns Response.error(404, "{}".toResponseBody())

        val result = repository.obtenerDetalle("laboratorio-1")

        assertEquals(NetworkResult.Failure(404, "gateway_http_404"), result)
    }

    @Test
    fun `fallo de red devuelve gateway_no_disponible`() = runTest {
        coEvery { api.obtenerDetalle(any()) } throws IOException("sin red")

        val result = repository.obtenerDetalle("laboratorio-1")

        assertEquals(NetworkResult.Failure(null, "gateway_no_disponible"), result)
    }

    @Test
    fun `excepcion inesperada devuelve respuesta_gateway_invalida`() = runTest {
        coEvery { api.obtenerDetalle(any()) } throws IllegalStateException("json invalido")

        val result = repository.obtenerDetalle("laboratorio-1")

        assertEquals(NetworkResult.Failure(null, "respuesta_gateway_invalida"), result)
    }

    private companion object {
        val DTO = LaboratorioDetalleDto(
            laboratorio = LaboratorioDto(
                id = "laboratorio-1", codigo = "LAB-1", nombre = "Redes", capacidad = 30,
                descripcion = null, estado = "ACTIVO", activo = true,
            ),
            piso = null,
            bloque = null,
            campus = null,
            equipos = emptyList(),
        )
    }
}
