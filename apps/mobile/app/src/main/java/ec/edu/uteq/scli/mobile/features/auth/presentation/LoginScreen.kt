package ec.edu.uteq.scli.mobile.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("SCLI", style = MaterialTheme.typography.headlineLarge)
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario o correo") },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            singleLine = true,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
        )
        state.error?.let { error ->
            Text(
                text = when (error) {
                    "credenciales_invalidas" -> "Credenciales inválidas"
                    "error_red" -> "No se pudo conectar con el servicio"
                    else -> "El servicio no está disponible"
                },
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Button(
            onClick = { viewModel.login(username, password) },
            enabled = !state.cargando,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (state.cargando) CircularProgressIndicator()
            else Text("Iniciar sesión")
        }
    }
}
