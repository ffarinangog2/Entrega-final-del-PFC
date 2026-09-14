package ec.edu.uteq.scli.mobile.features.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.R

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarConfirmacionLogout by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.perfil_titulo),
            style = MaterialTheme.typography.titleLarge,
        )

        OutlinedTextField(
            value = uiState.nombreUsuario,
            onValueChange = viewModel::onNombreChange,
            label = { Text(stringResource(R.string.perfil_nombre_label)) },
            readOnly = uiState.perfilRemoto,
            modifier = Modifier.fillMaxWidth(),
        )
        if (uiState.perfilRemoto) {
            OutlinedTextField(
                value = uiState.emailInstitucional,
                onValueChange = {},
                readOnly = true,
                label = { Text("Correo institucional") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.emailPersonal,
                onValueChange = viewModel::onEmailPersonalChange,
                label = { Text("Correo personal") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.telefono,
                onValueChange = viewModel::onTelefonoChange,
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.direccion,
                onValueChange = viewModel::onDireccionChange,
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.fotoUrl,
                onValueChange = viewModel::onFotoUrlChange,
                label = { Text("URL de foto") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::guardarPerfil,
                enabled = !uiState.guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.guardando) "Guardando…" else "Guardar perfil")
            }
            uiState.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.perfil_settings_titulo),
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Notificaciones de incidentes",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Recibir alertas de nuevos reportes de incidentes en los laboratorios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Switch(
                checked = uiState.notificacionesHabilitadas,
                onCheckedChange = viewModel::onToggleNotificaciones,
            )
        }

        TemaSelector(temaOscuro = uiState.temaOscuro, onTemaChange = viewModel::onTemaChange)

        IdiomaSelector(idiomaApp = uiState.idiomaApp, onIdiomaChange = viewModel::onIdiomaChange)

        Button(
            onClick = { mostrarConfirmacionLogout = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text(text = stringResource(R.string.auth_logout))
        }
    }

    if (mostrarConfirmacionLogout) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionLogout = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar tu sesión en este dispositivo?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionLogout = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionLogout = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun TemaSelector(temaOscuro: Boolean?, onTemaChange: (Boolean?) -> Unit) {
    Column {
        Text(text = stringResource(R.string.perfil_settings_tema_titulo))
        Column(Modifier.selectableGroup()) {
            OpcionRadio(
                seleccionado = temaOscuro == null,
                texto = stringResource(R.string.perfil_settings_tema_sistema),
                onClick = { onTemaChange(null) },
            )
            OpcionRadio(
                seleccionado = temaOscuro == false,
                texto = stringResource(R.string.perfil_settings_tema_claro),
                onClick = { onTemaChange(false) },
            )
            OpcionRadio(
                seleccionado = temaOscuro == true,
                texto = stringResource(R.string.perfil_settings_tema_oscuro),
                onClick = { onTemaChange(true) },
            )
        }
    }
}

@Composable
private fun IdiomaSelector(idiomaApp: String?, onIdiomaChange: (String?) -> Unit) {
    Column {
        Text(text = stringResource(R.string.perfil_settings_idioma_titulo))
        Column(Modifier.selectableGroup()) {
            OpcionRadio(
                seleccionado = idiomaApp == null,
                texto = stringResource(R.string.perfil_settings_idioma_sistema),
                onClick = { onIdiomaChange(null) },
            )
            OpcionRadio(
                seleccionado = idiomaApp == "es",
                texto = stringResource(R.string.perfil_settings_idioma_es),
                onClick = { onIdiomaChange("es") },
            )
            OpcionRadio(
                seleccionado = idiomaApp == "en",
                texto = stringResource(R.string.perfil_settings_idioma_en),
                onClick = { onIdiomaChange("en") },
            )
        }
    }
}

@Composable
private fun OpcionRadio(seleccionado: Boolean, texto: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionado, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = seleccionado, onClick = null)
        Text(text = texto, modifier = Modifier.padding(start = 8.dp))
    }
}
