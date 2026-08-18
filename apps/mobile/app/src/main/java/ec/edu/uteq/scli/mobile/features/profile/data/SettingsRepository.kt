package ec.edu.uteq.scli.mobile.features.profile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "scli_settings")

/**
 * Preferencias del técnico (perfil + settings), persistidas con Jetpack
 * DataStore en vez de SharedPreferences.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val NOMBRE_TECNICO = stringPreferencesKey("nombre_tecnico")
        val NOTIFICACIONES_HABILITADAS = booleanPreferencesKey("notificaciones_habilitadas")
    }

    val nombreTecnico: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOMBRE_TECNICO] ?: ""
    }

    val notificacionesHabilitadas: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICACIONES_HABILITADAS] ?: true
    }

    suspend fun setNombreTecnico(nombre: String) {
        context.dataStore.edit { prefs -> prefs[Keys.NOMBRE_TECNICO] = nombre }
    }

    suspend fun setNotificacionesHabilitadas(habilitadas: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.NOTIFICACIONES_HABILITADAS] = habilitadas }
    }
}
