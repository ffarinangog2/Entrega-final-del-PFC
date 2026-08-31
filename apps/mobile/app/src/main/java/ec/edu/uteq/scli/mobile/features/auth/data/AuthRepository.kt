package ec.edu.uteq.scli.mobile.features.auth.data

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import java.io.IOException

interface AuthRepository {
    suspend fun login(username: String, password: String): NetworkResult<AuthSession>
    fun restoreSession(): AuthSession?
    suspend fun refreshSession(): AuthSession? = null
    suspend fun logout()
    fun onSessionExpired(listener: () -> Unit) {}
    fun onAuthenticated(listener: suspend () -> Unit) {}
}

interface SecureTokenStorage {
    fun save(session: AuthSession)
    fun read(): AuthSession?
    fun clear()
}

class RemoteAuthRepository(
    private val api: AuthApi,
    private val storage: SecureTokenStorage,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : AuthRepository {
    private var sessionExpiredListener: () -> Unit = {}
    private var authenticatedListener: suspend () -> Unit = {}
    override suspend fun login(username: String, password: String): NetworkResult<AuthSession> = try {
        val response = api.login(LoginRequest(username, password))
        val body = response.body()
        when {
            response.isSuccessful && body != null -> {
                val session = AuthSession(
                    tokenType = body.tokenType,
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    expiresAtMillis = clock() + body.expiresIn * 1000,
                    usuario = body.usuario,
                )
                storage.save(session)
                authenticatedListener()
                NetworkResult.Success(session)
            }
            response.code() == 401 || response.code() == 403 ->
                NetworkResult.Failure(response.code(), "credenciales_invalidas")
            response.code() == 423 -> NetworkResult.Failure(423, "cuenta_bloqueada")
            else -> NetworkResult.Failure(response.code(), "servicio_no_disponible")
        }
    } catch (_: IOException) {
        NetworkResult.Failure(null, "error_red")
    } catch (_: RuntimeException) {
        NetworkResult.Failure(null, "servicio_no_disponible")
    }

    override fun restoreSession(): AuthSession? = storage.read()?.takeIf { it.expiresAtMillis > clock() }

    override suspend fun refreshSession(): AuthSession? {
        val current = storage.read() ?: return null
        return try {
            val response = api.refresh(RefreshRequest(current.refreshToken))
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                storage.clear()
                sessionExpiredListener()
                null
            } else {
                AuthSession(
                    body.tokenType, body.accessToken, body.refreshToken,
                    clock() + body.expiresIn * 1000, body.usuario,
                ).also(storage::save)
                    .also { authenticatedListener() }
            }
        } catch (_: IOException) {
            storage.clear()
            sessionExpiredListener()
            null
        } catch (_: RuntimeException) {
            storage.clear()
            sessionExpiredListener()
            null
        }
    }

    override suspend fun logout() {
        val current = storage.read()
        try {
            if (current != null) api.logout(RefreshRequest(current.refreshToken))
        } catch (_: IOException) {
            // El cierre local nunca debe depender de la disponibilidad de red.
        } catch (_: RuntimeException) {
            // Una respuesta remota defectuosa tampoco debe bloquear el cierre local.
        } finally {
            storage.clear()
        }
    }
    override fun onSessionExpired(listener: () -> Unit) { sessionExpiredListener = listener }
    override fun onAuthenticated(listener: suspend () -> Unit) { authenticatedListener = listener }
}
