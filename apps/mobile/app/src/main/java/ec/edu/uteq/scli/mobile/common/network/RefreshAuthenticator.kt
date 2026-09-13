package ec.edu.uteq.scli.mobile.common.network

import ec.edu.uteq.scli.mobile.features.auth.data.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Coordina el refresh para todas las llamadas del cliente Gateway. */
class RefreshAuthenticator(
    private val repository: AuthRepository,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val failedToken = response.request.header("Authorization")
        synchronized(lock) {
            val current = repository.restoreSession()
            val currentHeader = current?.accessToken?.let { "Bearer $it" }
            val token = if (currentHeader != null && currentHeader != failedToken) {
                current.accessToken
            } else {
                runBlocking { repository.refreshSession() }?.accessToken ?: return null
            }
            return response.request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
