package ec.edu.uteq.scli.mobile.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ScliLightColorScheme = lightColorScheme(
    background = ScliLightBg,
    onBackground = ScliLightText,
    surface = ScliLightBgCard,
    onSurface = ScliLightText,
    surfaceVariant = ScliLightBgCard,
    onSurfaceVariant = ScliLightText,
    primary = ScliLightPrimary,
    onPrimary = ScliLightBgCard,
    primaryContainer = ScliLightPrimary,
    onPrimaryContainer = ScliLightBgCard,
    secondary = ScliLightPrimary,
    onSecondary = ScliLightBgCard,
    secondaryContainer = ScliLightPrimary,
    onSecondaryContainer = ScliLightBgCard,
    tertiary = ScliLightPrimary,
    onTertiary = ScliLightBgCard,
    outline = ScliLightBorder,
    outlineVariant = ScliLightBorder,
    error = ScliLightDanger,
    onError = ScliLightDangerBg,
    errorContainer = ScliLightDangerBg,
    onErrorContainer = ScliLightDangerText,
)

private val ScliDarkColorScheme = darkColorScheme(
    background = ScliDarkBg,
    onBackground = ScliDarkText,
    surface = ScliDarkBgCard,
    onSurface = ScliDarkText,
    surfaceVariant = ScliDarkBgCard,
    onSurfaceVariant = ScliDarkText,
    primary = ScliDarkPrimary,
    onPrimary = ScliDarkBg,
    primaryContainer = ScliDarkPrimary,
    onPrimaryContainer = ScliDarkBg,
    secondary = ScliDarkPrimary,
    onSecondary = ScliDarkBg,
    secondaryContainer = ScliDarkPrimary,
    onSecondaryContainer = ScliDarkBg,
    tertiary = ScliDarkPrimary,
    onTertiary = ScliDarkBg,
    outline = ScliDarkBorder,
    outlineVariant = ScliDarkBorder,
    error = ScliDarkDanger,
    onError = ScliDarkDangerBg,
    errorContainer = ScliDarkDangerBg,
    onErrorContainer = ScliDarkDangerText,
)

/** Identidad visual de SCLI (verde bosque sobre fondo crema) para Compose. */
@Composable
fun ScliTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) ScliDarkColorScheme else ScliLightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
