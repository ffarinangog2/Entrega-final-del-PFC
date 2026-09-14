package ec.edu.uteq.scli.mobile.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.scli.mobile.common.theme.ScliAccent
import ec.edu.uteq.scli.mobile.common.theme.ScliAccentText

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var mostrarRecuperacion by rememberSaveable { mutableStateOf(false) }
    var identifierRecuperacion by rememberSaveable { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        LoginCard(
            username = username,
            onUsernameChange = { username = it },
            password = password,
            onPasswordChange = { password = it },
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
            error = state.error,
            loading = state.cargando,
            onSubmit = { viewModel.login(username, password) },
            onForgotPasswordClick = { mostrarRecuperacion = true },
        )
    }

    if (mostrarRecuperacion) {
        AlertDialog(
            onDismissRequest = {
                mostrarRecuperacion = false
                viewModel.limpiarEstadoRecuperacion()
            },
            title = { Text("Recuperación de contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ingresa tu usuario o correo institucional para recibir las instrucciones de restablecimiento.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = identifierRecuperacion,
                        onValueChange = { identifierRecuperacion = it },
                        label = { Text("Usuario o correo institucional") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.recuperacionCargando,
                    )
                    if (state.recuperacionCargando) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    state.recuperacionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    state.recuperacionMensaje?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Por seguridad institucional, el enlace recibido en tu correo debe abrirse desde el navegador web para establecer tu nueva contraseña.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            },
            confirmButton = {
                if (state.recuperacionMensaje == null) {
                    Button(
                        onClick = { viewModel.solicitarRecuperacion(identifierRecuperacion) },
                        enabled = identifierRecuperacion.isNotBlank() && !state.recuperacionCargando,
                    ) {
                        Text("Enviar solicitud")
                    }
                } else {
                    Button(onClick = {
                        mostrarRecuperacion = false
                        viewModel.limpiarEstadoRecuperacion()
                    }) {
                        Text("Entendido")
                    }
                }
            },
            dismissButton = {
                if (state.recuperacionMensaje == null) {
                    TextButton(onClick = {
                        mostrarRecuperacion = false
                        viewModel.limpiarEstadoRecuperacion()
                    }) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }
}

@Composable
private fun LoginCard(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    error: String?,
    loading: Boolean,
    onSubmit: () -> Unit,
    onForgotPasswordClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 28.dp, vertical = 32.dp),
    ) {
        LoginBrandRow()

        Spacer(Modifier.height(28.dp))

        Text(
            text = "ACCESO INSTITUCIONAL",
            color = ScliAccentText,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
        )
        Text(
            text = "SCLI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "Sistema de Control de Laboratorios Informáticos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 10.dp),
        )

        Spacer(Modifier.height(26.dp))

        LoginLabeledField(
            label = "Usuario o correo",
            value = username,
            onValueChange = onUsernameChange,
        )

        Spacer(Modifier.height(16.dp))

        LoginLabeledField(
            label = "Contraseña",
            value = password,
            onValueChange = onPasswordChange,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onForgotPasswordClick) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            LoginErrorBanner(mapLoginError(error))
        }

        Spacer(Modifier.height(16.dp))

        LoginSubmitButton(loading = loading, onClick = onSubmit)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            text = "UTEQ · Aplicaciones Distribuidas",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Composable
private fun LoginBrandRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box {
            // Sombra sólida sin blur (equivalente a box-shadow: 4px 4px 0 #d08a3d).
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = 4.dp)
                    .size(38.dp)
                    .background(ScliAccent, RoundedCornerShape(10.dp)),
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "S",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
        }
        Column {
            Text(
                text = "SCLI",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
            )
            Text(
                text = "UTEQ",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun LoginLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (isPassword) {
                KeyboardOptions(keyboardType = KeyboardType.Password)
            } else {
                KeyboardOptions.Default
            },
            trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
                {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun LoginErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.error),
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun LoginSubmitButton(loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "Iniciar sesión",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

private fun mapLoginError(code: String): String = when (code) {
    "credenciales_invalidas" -> "Usuario o contraseña incorrectos"
    "cuenta_bloqueada" -> "Cuenta bloqueada temporalmente por intentos fallidos. Intenta más tarde."
    "error_red" -> "No se pudo conectar con el servicio"
    else -> "Error al iniciar sesión ($code)"
}
