package ec.edu.uteq.scli.mobile.features.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.R

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.perfil_titulo))

        OutlinedTextField(
            value = uiState.nombreUsuario,
            onValueChange = viewModel::onNombreChange,
            label = { Text(stringResource(R.string.perfil_nombre_label)) },
            readOnly = uiState.perfilRemoto,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        Text(text = stringResource(R.string.perfil_settings_titulo))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.perfil_settings_notificaciones))
            Switch(
                checked = uiState.notificacionesHabilitadas,
                onCheckedChange = viewModel::onToggleNotificaciones,
            )
        }

        TemaSelector(temaOscuro = uiState.temaOscuro, onTemaChange = viewModel::onTemaChange)

        IdiomaSelector(idiomaApp = uiState.idiomaApp, onIdiomaChange = viewModel::onIdiomaChange)

        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.auth_logout))
        }
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
