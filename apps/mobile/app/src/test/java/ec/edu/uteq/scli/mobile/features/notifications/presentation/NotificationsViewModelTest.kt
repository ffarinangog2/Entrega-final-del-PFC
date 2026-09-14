package ec.edu.uteq.scli.mobile.features.notifications.presentation

import ec.edu.uteq.scli.mobile.features.notifications.data.NotificacionInternaResponse
import ec.edu.uteq.scli.mobile.features.notifications.data.NotificationsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: NotificationsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `E - cargar llena lista y contador`() = runTest {
        val items = listOf(
            NotificacionInternaResponse("1", "Titulo 1", "Cuerpo 1", "TIPO", null, leida = false, creadaEn = "2026-09-08T00:00:00Z"),
            NotificacionInternaResponse("2", "Titulo 2", "Cuerpo 2", "TIPO", null, leida = true, creadaEn = "2026-09-08T00:00:00Z"),
        )
        coEvery { repository.listar() } returns Result.success(items)
        coEvery { repository.contarNoLeidas() } returns Result.success(1L)

        val viewModel = NotificationsViewModel(repository)
        viewModel.cargar()

        assertFalse(viewModel.uiState.value.loading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.items.size)
        assertEquals(1L, viewModel.uiState.value.cantidadNoLeidas)
    }

    @Test
    fun `F - marcar una leida=true y contador disminuye solo si antes estaba no leida`() = runTest {
        val items = listOf(
            NotificacionInternaResponse("1", "Titulo 1", "Cuerpo 1", "TIPO", null, leida = false, creadaEn = "2026-09-08T00:00:00Z"),
            NotificacionInternaResponse("2", "Titulo 2", "Cuerpo 2", "TIPO", null, leida = false, creadaEn = "2026-09-08T00:00:00Z"),
        )
        coEvery { repository.listar() } returns Result.success(items)
        coEvery { repository.contarNoLeidas() } returns Result.success(2L)
        coEvery { repository.marcarLeida("1") } returns Result.success(items[0].copy(leida = true))

        val viewModel = NotificationsViewModel(repository)
        viewModel.cargar()
        assertEquals(2L, viewModel.uiState.value.cantidadNoLeidas)

        viewModel.marcarLeida("1")

        assertTrue(viewModel.uiState.value.items.first { it.id == "1" }.leida)
        assertEquals(1L, viewModel.uiState.value.cantidadNoLeidas)
        coVerify(exactly = 1) { repository.marcarLeida("1") }
    }

    @Test
    fun `G - marcar una ya leida no reduce el contador otra vez`() = runTest {
        val items = listOf(
            NotificacionInternaResponse("1", "Titulo 1", "Cuerpo 1", "TIPO", null, leida = true, creadaEn = "2026-09-08T00:00:00Z"),
        )
        coEvery { repository.listar() } returns Result.success(items)
        coEvery { repository.contarNoLeidas() } returns Result.success(0L)

        val viewModel = NotificationsViewModel(repository)
        viewModel.cargar()
        assertEquals(0L, viewModel.uiState.value.cantidadNoLeidas)

        viewModel.marcarLeida("1")

        // No debe llamar al repositorio porque ya estaba leída
        coVerify(exactly = 0) { repository.marcarLeida(any()) }
        assertEquals(0L, viewModel.uiState.value.cantidadNoLeidas)
    }

    @Test
    fun `H - marcar todas deja todas leidas y contador en 0`() = runTest {
        val items = listOf(
            NotificacionInternaResponse("1", "Titulo 1", "Cuerpo 1", "TIPO", null, leida = false, creadaEn = "2026-09-08T00:00:00Z"),
            NotificacionInternaResponse("2", "Titulo 2", "Cuerpo 2", "TIPO", null, leida = false, creadaEn = "2026-09-08T00:00:00Z"),
        )
        coEvery { repository.listar() } returns Result.success(items)
        coEvery { repository.contarNoLeidas() } returns Result.success(2L)
        coEvery { repository.marcarTodasLeidas() } returns Result.success(Unit)

        val viewModel = NotificationsViewModel(repository)
        viewModel.cargar()
        assertEquals(2L, viewModel.uiState.value.cantidadNoLeidas)

        viewModel.marcarTodas()

        assertTrue(viewModel.uiState.value.items.all { it.leida })
        assertEquals(0L, viewModel.uiState.value.cantidadNoLeidas)
        coVerify(exactly = 1) { repository.marcarTodasLeidas() }
    }

    @Test
    fun `I - error de red no hace crash y actualiza error`() = runTest {
        coEvery { repository.listar() } returns Result.failure(RuntimeException("Error de conexión"))
        coEvery { repository.contarNoLeidas() } returns Result.failure(RuntimeException("Error de conexión"))

        val viewModel = NotificationsViewModel(repository)
        viewModel.cargar()

        assertFalse(viewModel.uiState.value.loading)
        assertEquals("Error de conexión", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun `cargarNoLeidas actualiza silenciosamente el conteo`() = runTest {
        coEvery { repository.contarNoLeidas() } returns Result.success(5L)

        val viewModel = NotificationsViewModel(repository)
        viewModel.cargarNoLeidas()

        assertEquals(5L, viewModel.uiState.value.cantidadNoLeidas)
    }

    @Test
    fun `N - notificacion ya leida no vuelve a llamar POST leer y ejecuta onExito`() = runTest {
        val items = listOf(
            NotificacionInternaResponse("1", "Titulo 1", "Cuerpo 1", "TIPO", null, leida = true, creadaEn = "2026-09-08T00:00:00Z"),
        )
        coEvery { repository.listar() } returns Result.success(items)
        coEvery { repository.contarNoLeidas() } returns Result.success(0L)

        val viewModel = NotificationsViewModel(repository)
        viewModel.cargar()

        var exitoEjecutado = false
        viewModel.marcarLeida("1") {
            exitoEjecutado = true
        }

        coVerify(exactly = 0) { repository.marcarLeida(any()) }
        assertTrue(exitoEjecutado)
    }

    @Test
    fun `O - fallo al marcar leida no ejecuta onExito y no navega`() = runTest {
        val items = listOf(
            NotificacionInternaResponse("1", "Titulo 1", "Cuerpo 1", "TIPO", null, leida = false, creadaEn = "2026-09-08T00:00:00Z"),
        )
        coEvery { repository.listar() } returns Result.success(items)
        coEvery { repository.contarNoLeidas() } returns Result.success(1L)
        coEvery { repository.marcarLeida("1") } returns Result.failure(RuntimeException("Error en servidor"))

        val viewModel = NotificationsViewModel(repository)
        viewModel.cargar()

        var exitoEjecutado = false
        viewModel.marcarLeida("1") {
            exitoEjecutado = true
        }

        coVerify(exactly = 1) { repository.marcarLeida("1") }
        assertFalse(exitoEjecutado)
        assertFalse(viewModel.uiState.value.items.first().leida)
        assertEquals("Error en servidor", viewModel.uiState.value.error)
    }
}
