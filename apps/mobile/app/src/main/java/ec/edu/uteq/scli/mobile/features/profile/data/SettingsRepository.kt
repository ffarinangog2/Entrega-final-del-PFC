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
        val TEMA_OSCURO = booleanPreferencesKey("tema_oscuro")
        val IDIOMA_APP = stringPreferencesKey("idioma_app")
    }

    val nombreTecnico: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOMBRE_TECNICO] ?: ""
    }

    val notificacionesHabilitadas: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICACIONES_HABILITADAS] ?: true
    }

    /** null = seguir el tema del sistema; true = oscuro forzado; false = claro forzado. */
    val temaOscuro: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[Keys.TEMA_OSCURO]
    }

    /** null = seguir el idioma del sistema; "es"/"en" = idioma forzado. */
    val idiomaApp: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.IDIOMA_APP]
    }

    suspend fun setNombreTecnico(nombre: String) {
        context.dataStore.edit { prefs -> prefs[Keys.NOMBRE_TECNICO] = nombre }
    }

    suspend fun setNotificacionesHabilitadas(habilitadas: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.NOTIFICACIONES_HABILITADAS] = habilitadas }
    }

    suspend fun setTemaOscuro(oscuro: Boolean?) {
        context.dataStore.edit { prefs ->
            if (oscuro == null) prefs.remove(Keys.TEMA_OSCURO) else prefs[Keys.TEMA_OSCURO] = oscuro
        }
    }

    suspend fun setIdiomaApp(idioma: String?) {
        context.dataStore.edit { prefs ->
            if (idioma == null) prefs.remove(Keys.IDIOMA_APP) else prefs[Keys.IDIOMA_APP] = idioma
        }
    }
}
