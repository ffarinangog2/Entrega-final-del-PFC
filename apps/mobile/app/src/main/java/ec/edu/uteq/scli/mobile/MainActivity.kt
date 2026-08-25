package ec.edu.uteq.scli.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ec.edu.uteq.scli.mobile.common.navigation.AppNavHost
import ec.edu.uteq.scli.mobile.common.theme.ScliTheme
import ec.edu.uteq.scli.mobile.features.notifications.RequestNotificationPermissionEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScliTheme {
                RequestNotificationPermissionEffect()
                AppNavHost(application as ScliMobileApplication)
            }
        }
    }
}
