package ec.edu.uteq.scli.mobile.features.qr.presentation

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.qr.data.Equipo
import ec.edu.uteq.scli.mobile.features.qr.data.Laboratorio
import ec.edu.uteq.scli.mobile.features.qr.data.LaboratorioDetalle
import ec.edu.uteq.scli.mobile.features.qr.data.QrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QrViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `UUID directo consulta detalle`() = runTest {
        val repository = FakeQrRepository()
        val viewModel = QrViewModel(repository)

        viewModel.procesarQr(LABORATORIO_ID)
        runCurrent()

        assertEquals(LABORATORIO_ID, repository.ultimoId)
        assertEquals(DETALLE, viewModel.uiState.value.detalle)
    }

    @Test
    fun `URL extrae UUID y evita procesar duplicado`() = runTest {
        val repository = FakeQrRepository()
        val viewModel = QrViewModel(repository)
        val url = "https://scli.example.edu/laboratorios/$LABORATORIO_ID?origen=qr"

        viewModel.procesarQr(url)
        viewModel.procesarQr(url)
        runCurrent()

        assertEquals(1, repository.consultas)
    }

    @Test
    fun `QR invalido expone error sin consultar`() = runTest {
        val repository = FakeQrRepository()
        val viewModel = QrViewModel(repository)

        viewModel.procesarQr("sala-101")
        runCurrent()

        assertEquals(QrError.INVALIDO, viewModel.uiState.value.error)
        assertEquals(0, repository.consultas)
    }

    @Test
    fun `reintentar permite escanear nuevamente`() = runTest {
        val repository = FakeQrRepository()
        val viewModel = QrViewModel(repository)

        viewModel.procesarQr(LABORATORIO_ID)
        runCurrent()
        viewModel.reintentar()
        viewModel.procesarQr(LABORATORIO_ID)
        runCurrent()

        assertEquals(2, repository.consultas)
        assertTrue(viewModel.uiState.value.detalle != null)
    }

    private class FakeQrRepository : QrRepository {
        var ultimoId: String? = null
        var consultas = 0

        override suspend fun obtenerDetalle(laboratorioId: String): NetworkResult<LaboratorioDetalle> {
            ultimoId = laboratorioId
            consultas++
            return NetworkResult.Success(DETALLE)
        }
    }

    private companion object {
        const val LABORATORIO_ID = "123e4567-e89b-12d3-a456-426614174000"
        val DETALLE = LaboratorioDetalle(
            laboratorio = Laboratorio(LABORATORIO_ID, "LAB-01", "Laboratorio 1", 20, null, "DISPONIBLE", true),
            piso = null,
            bloque = null,
            campus = null,
            equipos = listOf(Equipo("equipo-1", "EQ-01", null, "Marca", "Modelo", "OPERATIVO", true)),
        )
    }
}
