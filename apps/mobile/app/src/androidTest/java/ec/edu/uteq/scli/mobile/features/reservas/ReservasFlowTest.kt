package ec.edu.uteq.scli.mobile.features.reservas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservasContent
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservasUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReservasFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listadoMuestraReservaYAbreSuDetalle() {
        var reservaSeleccionada: String? = null
        composeRule.setContent {
            ReservasContent(
                state = ReservasUiState(reservas = listOf(RESERVA)),
                onRefresh = {},
                onRetry = {},
                onReservaClick = { reservaSeleccionada = it },
                onNuevaReserva = {},
            )
        }

        composeRule.onNodeWithText("Reservas").performClick()
        composeRule.onNodeWithTag("reserva_${RESERVA.id}").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(RESERVA.id, reservaSeleccionada) }
    }

    private companion object {
        val RESERVA = Reserva(
            "reserva-1", "solicitud-1", "laboratorio-1", "responsable-1",
            "2026-08-20", "08:00:00", "10:00:00", "PROGRAMADA", "RES-001",
            "2026-08-18T10:00:00Z", "2026-08-18T10:00:00Z", 0,
        )
    }
}
