package ec.edu.uteq.scli.mobile.features.auth.data

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import java.io.IOException

interface AuthRepository {
    suspend fun login(username: String, password: String): NetworkResult<AuthSession>
    fun restoreSession(): AuthSession?
    fun logout()
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
                NetworkResult.Success(session)
            }
            response.code() == 401 || response.code() == 403 ->
                NetworkResult.Failure(response.code(), "credenciales_invalidas")
            else -> NetworkResult.Failure(response.code(), "servicio_no_disponible")
        }
    } catch (_: IOException) {
        NetworkResult.Failure(null, "error_red")
    } catch (_: RuntimeException) {
        NetworkResult.Failure(null, "servicio_no_disponible")
    }

    override fun restoreSession(): AuthSession? = storage.read()?.takeIf { it.expiresAtMillis > clock() }

    override fun logout() = storage.clear()
}
