package ec.edu.uteq.scli.mobile

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import ec.edu.uteq.scli.mobile.common.navigation.AppNavHost
import ec.edu.uteq.scli.mobile.common.theme.ScliTheme
import ec.edu.uteq.scli.mobile.features.notifications.RequestNotificationPermissionEffect

/**
 * Sentinel para distinguir "el Flow de DataStore todavía no emitió" de
 * "emitió y el valor real es null" (que significa seguir el idioma del
 * sistema). Sin esto, cada recreate de la Activity arranca collectAsState
 * en null antes de que llegue el valor persistido real, y ese null
 * transitorio se interpreta como "forzar sistema", disparando otro
 * setApplicationLocales -> otro recreate -> loop infinito.
 */
private const val IDIOMA_SIN_CARGAR = " __sin_cargar__"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = (application as ScliMobileApplication).container.settingsRepository
        setContent {
            val temaOscuroPreferencia by settingsRepository.temaOscuro.collectAsState(initial = null)
            val idiomaAppPreferencia by settingsRepository.idiomaApp.collectAsState(initial = IDIOMA_SIN_CARGAR)

            LaunchedEffect(idiomaAppPreferencia) {
                if (idiomaAppPreferencia == IDIOMA_SIN_CARGAR) return@LaunchedEffect
                val locales = idiomaAppPreferencia?.let { LocaleListCompat.forLanguageTags(it) }
                    ?: LocaleListCompat.getEmptyLocaleList()
                if (AppCompatDelegate.getApplicationLocales() != locales) {
                    // setApplicationLocales fuerza un recreate() interno de la Activity
                    // (en API <34) para volver a resolver los recursos con el idioma
                    // nuevo. Envolvemos ese recreate con un fade corto para que no se
                    // sienta como un parpadeo brusco.
                    aplicarTransicionSuave()
                    AppCompatDelegate.setApplicationLocales(locales)
                }
            }

            ScliTheme(darkTheme = temaOscuroPreferencia ?: isSystemInDarkTheme()) {
                RequestNotificationPermissionEffect()
                AppNavHost(application as ScliMobileApplication)
            }
        }
    }

    private fun aplicarTransicionSuave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.fade_in_idioma,
                R.anim.fade_out_idioma,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in_idioma, R.anim.fade_out_idioma)
        }
    }
}
