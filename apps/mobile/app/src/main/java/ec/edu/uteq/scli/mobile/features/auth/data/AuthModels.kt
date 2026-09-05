package ec.edu.uteq.scli.mobile.features.auth.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


data class LoginRequest(
    val username: String,
    val password: String,
)

data class LoginResponse(
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val usuario: AuthUserResponse,
)

data class AuthUserResponse(
    val id: String,
    val perfilId: String,
    val username: String,
    val nombres: String,
    val apellidos: String,
    val emailInstitucional: String,
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList(),
    val tiposPerfil: List<String> = emptyList(),
)

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<LoginResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<Unit>
}

data class RefreshRequest(val refreshToken: String)

data class AuthSession(
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
    val usuario: AuthUserResponse,
)
