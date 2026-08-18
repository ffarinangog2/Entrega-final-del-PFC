package ec.edu.uteq.scli.mobile.common.di

import android.content.Context
import androidx.room.Room
import ec.edu.uteq.scli.mobile.data.local.AppDatabase
import ec.edu.uteq.scli.mobile.features.incidentes.data.IncidenteLocalRepository
import ec.edu.uteq.scli.mobile.features.incidentes.domain.IncidenteRepository
import ec.edu.uteq.scli.mobile.features.notifications.NotificationHelper
import ec.edu.uteq.scli.mobile.features.profile.data.SettingsRepository

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
    ).build()

    val incidenteRepository: IncidenteRepository = IncidenteLocalRepository(database.incidenteDao())

    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)

    val notificationHelper: NotificationHelper = NotificationHelper(context.applicationContext)
}
