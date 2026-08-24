package ec.edu.uteq.scli.mobile.features.qr

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.qr.data.Equipo
import ec.edu.uteq.scli.mobile.features.qr.data.Laboratorio
import ec.edu.uteq.scli.mobile.features.qr.data.LaboratorioDetalle
import ec.edu.uteq.scli.mobile.features.qr.data.QrRepository
import ec.edu.uteq.scli.mobile.features.qr.presentation.QrScanScreen
import ec.edu.uteq.scli.mobile.features.qr.presentation.QrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flujo de punta a punta de la feature QR. El resultado del escaneo real de
 * cámara no es determinista en un entorno de pruebas, así que se usa un
 * QrRepository doble y se deja el ViewModel con el estado ya resuelto antes
 * de componer la pantalla: así Compose nunca llega a montar la vista de
 * cámara real (CameraPreview) y la prueba queda estable en cualquier
 * dispositivo o emulador, tenga o no cámara funcional.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class QrFlowTest {

    @get:Rule
    val permisoCamaraRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun escanearQrValido_muestraDetalleDelLaboratorio() = runTest {
        val repository = FakeQrRepository(resultado = NetworkResult.Success(DETALLE))
        val viewModel = QrViewModel(repository)

        viewModel.procesarQr(LABORATORIO_ID)
        runCurrent()

        composeTestRule.setContent {
            MaterialTheme {
                QrScanScreen(viewModel)
            }
        }

        composeTestRule.onNodeWithText("Detalle del laboratorio").assertExists()
        composeTestRule.onNodeWithText(DETALLE.laboratorio.nombre).assertExists()
        composeTestRule.onNodeWithText("Código: ${DETALLE.laboratorio.codigo}").assertExists()
    }

    @Test
    fun qrInvalido_muestraErrorYPermiteReintentar() = runTest {
        val repository = FakeQrRepository(resultado = NetworkResult.Success(DETALLE))
        val viewModel = QrViewModel(repository)

        viewModel.procesarQr("no-es-un-uuid")
        runCurrent()

        composeTestRule.setContent {
            MaterialTheme {
                QrScanScreen(viewModel)
            }
        }

        composeTestRule
            .onNodeWithText("El QR no contiene un UUID de laboratorio válido.")
            .assertExists()

        composeTestRule.onNodeWithText("Reintentar escaneo").performClick()
        runCurrent()

        assertViewModelReiniciado(viewModel)
    }

    @Test
    fun errorDeRed_muestraMensajeDeGateway() = runTest {
        val repository = FakeQrRepository(resultado = NetworkResult.Failure(null, "gateway_no_disponible"))
        val viewModel = QrViewModel(repository)

        viewModel.procesarQr(LABORATORIO_ID)
        runCurrent()

        composeTestRule.setContent {
            MaterialTheme {
                QrScanScreen(viewModel)
            }
        }

        composeTestRule
            .onNodeWithText("No se pudo conectar con el Gateway.")
            .assertExists()
    }

    private fun assertViewModelReiniciado(viewModel: QrViewModel) {
        val estado = viewModel.uiState.value
        check(estado.detalle == null && estado.error == null) {
            "Se esperaba que 'reintentar' limpiara el estado, pero quedó: $estado"
        }
    }

    private class FakeQrRepository(
        private val resultado: NetworkResult<LaboratorioDetalle>,
    ) : QrRepository {
        override suspend fun obtenerDetalle(laboratorioId: String): NetworkResult<LaboratorioDetalle> = resultado
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