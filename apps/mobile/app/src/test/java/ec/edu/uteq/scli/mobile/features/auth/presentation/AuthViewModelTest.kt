package ec.edu.uteq.scli.mobile.features.auth.presentation

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.auth.data.AuthRepository
import ec.edu.uteq.scli.mobile.features.auth.data.AuthSession
import ec.edu.uteq.scli.mobile.features.auth.data.AuthUserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `login correcto autentica la sesion`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login("admin", "Admin123!")
        runCurrent()

        assertEquals(SESSION, viewModel.uiState.value.sesion)
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `credenciales invalidas muestran error`() = runTest {
        val repository = FakeAuthRepository().apply {
            loginResult = NetworkResult.Failure(401, "credenciales_invalidas")
        }
        val viewModel = AuthViewModel(repository)

        viewModel.login("admin", "incorrecta")
        runCurrent()

        assertEquals("credenciales_invalidas", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.sesion == null)
    }

    @Test
    fun `error de red se expone`() = runTest {
        val repository = FakeAuthRepository().apply {
            loginResult = NetworkResult.Failure(null, "error_red")
        }
        val viewModel = AuthViewModel(repository)

        viewModel.login("admin", "Admin123!")
        runCurrent()

        assertEquals("error_red", viewModel.uiState.value.error)
    }

    @Test
    fun `logout elimina la sesion`() = runTest {
        val repository = FakeAuthRepository().apply { restored = SESSION }
        val viewModel = AuthViewModel(repository)

        viewModel.logout()

        assertTrue(viewModel.uiState.value.sesion == null)
        assertTrue(repository.loggedOut)
    }

    private class FakeAuthRepository : AuthRepository {
        var loginResult: NetworkResult<AuthSession> = NetworkResult.Success(SESSION)
        var restored: AuthSession? = null
        var loggedOut = false

        override suspend fun login(username: String, password: String) = loginResult
        override fun restoreSession() = restored
        override suspend fun refreshSession() = restored
        override fun logout() { loggedOut = true }
    }

    private companion object {
        val SESSION = AuthSession(
            tokenType = "Bearer",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAtMillis = Long.MAX_VALUE,
            usuario = AuthUserResponse(
                id = "user-id",
                perfilId = "profile-id",
                username = "admin",
                nombres = "Admin",
                apellidos = "SCLI",
                emailInstitucional = "admin@example.edu",
            ),
        )
    }
}
