package ec.edu.uteq.scli.mobile.features.incidentes

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ec.edu.uteq.scli.mobile.R
import ec.edu.uteq.scli.mobile.data.local.AppDatabase
import ec.edu.uteq.scli.mobile.features.incidentes.data.IncidenteLocalRepository
import ec.edu.uteq.scli.mobile.features.incidentes.presentation.IncidentesScreen
import ec.edu.uteq.scli.mobile.features.incidentes.presentation.IncidentesViewModel
import ec.edu.uteq.scli.mobile.features.notifications.NotificationHelper
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flujo simple de punta a punta: crear un incidente desde el formulario y
 * verificar que aparece en el listado, usando una base Room en memoria.
 */
@RunWith(AndroidJUnit4::class)
class IncidentesFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun crearIncidente_apareceEnElListado() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repository = IncidenteLocalRepository(database.incidenteDao())
        val notificationHelper = NotificationHelper(context)
        val viewModel = IncidentesViewModel(repository, notificationHelper)

        composeTestRule.setContent {
            MaterialTheme {
                IncidentesScreen(viewModel)
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.incidentes_form_laboratorio_equipo))
            .performTextInput("Lab 3 - PC 12")

        composeTestRule
            .onNodeWithText(context.getString(R.string.incidentes_form_descripcion))
            .performTextInput("No enciende")

        composeTestRule
            .onNodeWithText(context.getString(R.string.incidentes_form_guardar))
            .performClick()

        composeTestRule
            .onNodeWithText("Lab 3 - PC 12")
            .assertExists()
    }
}
