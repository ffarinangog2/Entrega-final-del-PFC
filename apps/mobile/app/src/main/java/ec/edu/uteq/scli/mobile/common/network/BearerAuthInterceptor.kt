package ec.edu.uteq.scli.mobile.common.network

import okhttp3.Interceptor
import okhttp3.Response

/** Agrega el access token de la sesión actual cuando está disponible. */
class BearerAuthInterceptor(
    private val accessTokenProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = accessTokenProvider()?.trim().orEmpty()
        if (accessToken.isEmpty()) return chain.proceed(chain.request())

        val authenticatedRequest = chain.request().newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        return chain.proceed(authenticatedRequest)
    }
}
