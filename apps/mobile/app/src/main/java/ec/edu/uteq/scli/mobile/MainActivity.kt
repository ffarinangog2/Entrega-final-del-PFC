package ec.edu.uteq.scli.mobile

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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = (application as ScliMobileApplication).container.settingsRepository
        setContent {
            val temaOscuroPreferencia by settingsRepository.temaOscuro.collectAsState(initial = null)
            val idiomaAppPreferencia by settingsRepository.idiomaApp.collectAsState(initial = null)

            LaunchedEffect(idiomaAppPreferencia) {
                val locales = idiomaAppPreferencia?.let { LocaleListCompat.forLanguageTags(it) }
                    ?: LocaleListCompat.getEmptyLocaleList()
                AppCompatDelegate.setApplicationLocales(locales)
            }

            ScliTheme(darkTheme = temaOscuroPreferencia ?: isSystemInDarkTheme()) {
                RequestNotificationPermissionEffect()
                AppNavHost(application as ScliMobileApplication)
            }
        }
    }
}
