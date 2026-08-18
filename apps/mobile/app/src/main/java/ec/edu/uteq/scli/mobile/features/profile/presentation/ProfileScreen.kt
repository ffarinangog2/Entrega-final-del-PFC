package ec.edu.uteq.scli.mobile.features.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.R

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.perfil_titulo))

        OutlinedTextField(
            value = uiState.nombreTecnico,
            onValueChange = viewModel::onNombreChange,
            label = { Text(stringResource(R.string.perfil_nombre_label)) },
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
    }
}
