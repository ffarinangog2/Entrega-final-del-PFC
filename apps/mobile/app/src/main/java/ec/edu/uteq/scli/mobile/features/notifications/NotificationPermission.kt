package ec.edu.uteq.scli.mobile.features.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Pide el permiso POST_NOTIFICATIONS (obligatorio desde Android 13 / API 33)
 * la primera vez que se compone la pantalla. En versiones anteriores no hace
 * falta pedir nada: las notificaciones están habilitadas por defecto.
 */
@Composable
fun RequestNotificationPermissionEffect() {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Timber.tag("NotificationPermission").i("POST_NOTIFICATIONS concedido=%s", granted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect

        val yaConcedido = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!yaConcedido) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
