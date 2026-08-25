package ec.edu.uteq.scli.mobile.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.features.profile.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.nombreTecnico,
        settingsRepository.notificacionesHabilitadas,
        settingsRepository.temaOscuro,
        settingsRepository.idiomaApp,
    ) { nombre, notificaciones, temaOscuro, idiomaApp ->
        ProfileUiState(
            nombreTecnico = nombre,
            notificacionesHabilitadas = notificaciones,
            temaOscuro = temaOscuro,
            idiomaApp = idiomaApp,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun onNombreChange(nombre: String) {
        viewModelScope.launch {
            settingsRepository.setNombreTecnico(nombre)
            Timber.tag("ProfileViewModel").d("Nombre del técnico actualizado")
        }
    }

    fun onToggleNotificaciones(habilitadas: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificacionesHabilitadas(habilitadas)
            Timber.tag("ProfileViewModel").d("Notificaciones habilitadas=%s", habilitadas)
        }
    }

    fun onTemaChange(oscuro: Boolean?) {
        viewModelScope.launch {
            settingsRepository.setTemaOscuro(oscuro)
            Timber.tag("ProfileViewModel").d("Tema oscuro=%s", oscuro)
        }
    }

    fun onIdiomaChange(idioma: String?) {
        viewModelScope.launch {
            settingsRepository.setIdiomaApp(idioma)
            Timber.tag("ProfileViewModel").d("Idioma app=%s", idioma)
        }
    }
}
