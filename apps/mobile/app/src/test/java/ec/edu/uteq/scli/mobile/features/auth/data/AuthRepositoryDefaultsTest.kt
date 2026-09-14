package ec.edu.uteq.scli.mobile.features.auth.data

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryDefaultsTest {
    @Test
    fun `refreshSession por defecto no esta implementado y devuelve null`() = runTest {
        val repository = object : AuthRepository {
            override suspend fun login(username: String, password: String) =
                NetworkResult.Failure(null, "no_usado")
            override fun restoreSession(): AuthSession? = null
            override suspend fun logout() {}
        }

        assertNull(repository.refreshSession())
    }
}
