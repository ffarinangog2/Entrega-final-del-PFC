package ec.edu.uteq.scli.mobile.common.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GatewayClientFactory {
    fun createRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient = OkHttpClient.Builder().build(),
        gson: Gson = GsonBuilder().create(),
    ): Retrofit {
        require(baseUrl.endsWith('/')) { "La URL base del Gateway debe terminar en /" }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
