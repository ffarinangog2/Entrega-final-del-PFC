package ec.edu.uteq.scli.mobile.common.di

import android.content.Context
import androidx.room.Room
import ec.edu.uteq.scli.mobile.data.local.AppDatabase
import ec.edu.uteq.scli.mobile.BuildConfig
import ec.edu.uteq.scli.mobile.common.network.BearerAuthInterceptor
import ec.edu.uteq.scli.mobile.common.network.GatewayClientFactory
import ec.edu.uteq.scli.mobile.features.auth.data.AuthApi
import ec.edu.uteq.scli.mobile.features.auth.data.AuthRepository
import ec.edu.uteq.scli.mobile.features.auth.data.EncryptedAuthStorage
import ec.edu.uteq.scli.mobile.features.auth.data.RemoteAuthRepository
import ec.edu.uteq.scli.mobile.features.incidentes.data.IncidenteLocalRepository
import ec.edu.uteq.scli.mobile.features.incidentes.domain.IncidenteRepository
import ec.edu.uteq.scli.mobile.features.notifications.NotificationHelper
import ec.edu.uteq.scli.mobile.features.profile.data.SettingsRepository
import ec.edu.uteq.scli.mobile.features.qr.data.RemoteQrRepository
import ec.edu.uteq.scli.mobile.features.qr.data.QrRepository
import ec.edu.uteq.scli.mobile.features.qr.data.QrApi
import ec.edu.uteq.scli.mobile.features.reservas.data.RemoteReservaRepository
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservasApi
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import okhttp3.OkHttpClient

/**
 * Contenedor de dependencias manual (sin Hilt) para mantener el scaffold
 * simple. Se instancia una vez en [ec.edu.uteq.scli.mobile.ScliMobileApplication]
 * y se comparte desde ahí.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "scli-mobile.db",
    ).addMigrations(AppDatabase.MIGRATION_1_2).build()

    val incidenteRepository: IncidenteRepository = IncidenteLocalRepository(database.incidenteDao())

    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)

    val notificationHelper: NotificationHelper = NotificationHelper(context.applicationContext)

    private val authStorage = EncryptedAuthStorage(context.applicationContext)
    private val gatewayRetrofit = GatewayClientFactory.createRetrofit(BuildConfig.API_BASE_URL)
    val authRepository: AuthRepository = RemoteAuthRepository(
        gatewayRetrofit.create(AuthApi::class.java),
        authStorage,
    )
    private val authenticatedGatewayRetrofit = GatewayClientFactory.createRetrofit(
        BuildConfig.API_BASE_URL,
        OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor {
                authRepository.restoreSession()?.accessToken
            })
            .build(),
    )
    private val reservasApi = authenticatedGatewayRetrofit.create(ReservasApi::class.java)
    val reservaRepository: ReservaRepository = RemoteReservaRepository(reservasApi, database.reservaDao())
    private val qrApi = authenticatedGatewayRetrofit.create(QrApi::class.java)
    val qrRepository: QrRepository = RemoteQrRepository(qrApi)
}
