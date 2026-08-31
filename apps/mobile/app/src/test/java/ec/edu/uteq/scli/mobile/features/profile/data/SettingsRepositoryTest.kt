package ec.edu.uteq.scli.mobile.features.profile.data

import android.app.Application
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsRepositoryTest {
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() = runTest {
        val application = RuntimeEnvironment.getApplication()
        repository = SettingsRepository(application)
        resetPreferences()
    }

    @After
    fun tearDown() = runTest {
        resetPreferences()
    }

    @Test
    fun `returns documented defaults when preferences are absent`() = runTest {
        assertEquals("", repository.nombreUsuario.first())
        assertTrue(repository.notificacionesHabilitadas.first())
        assertNull(repository.temaOscuro.first())
        assertNull(repository.idiomaApp.first())
    }

    @Test
    fun `persists profile and notification preferences`() = runTest {
        repository.setNombreUsuario("Usuario de laboratorio")
        repository.setNotificacionesHabilitadas(false)

        assertEquals("Usuario de laboratorio", repository.nombreUsuario.first())
        assertFalse(repository.notificacionesHabilitadas.first())
    }

    @Test
    fun `persists and removes optional appearance preferences`() = runTest {
        repository.setTemaOscuro(true)
        repository.setIdiomaApp("en")
        assertEquals(true, repository.temaOscuro.first())
        assertEquals("en", repository.idiomaApp.first())

        repository.setTemaOscuro(null)
        repository.setIdiomaApp(null)
        assertNull(repository.temaOscuro.first())
        assertNull(repository.idiomaApp.first())
    }

    private suspend fun resetPreferences() {
        repository.setNombreUsuario("")
        repository.setNotificacionesHabilitadas(true)
        repository.setTemaOscuro(null)
        repository.setIdiomaApp(null)
    }
}
