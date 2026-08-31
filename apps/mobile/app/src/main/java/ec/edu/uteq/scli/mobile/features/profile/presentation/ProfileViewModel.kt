package ec.edu.uteq.scli.mobile.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.features.profile.data.SettingsRepository
import ec.edu.uteq.scli.mobile.features.profile.data.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository? = null,
) : ViewModel() {

    private val nombreRemoto = MutableStateFlow<String?>(null)

    init {
        if (profileRepository != null) viewModelScope.launch {
            runCatching { profileRepository.getOwnProfile() }
                .onSuccess { nombreRemoto.value = "${it.nombres} ${it.apellidos}".trim() }
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.nombreTecnico,
        settingsRepository.notificacionesHabilitadas,
        settingsRepository.temaOscuro,
        settingsRepository.idiomaApp,
        nombreRemoto,
    ) { nombre, notificaciones, temaOscuro, idiomaApp, remoto ->
        ProfileUiState(
            nombreTecnico = remoto ?: nombre,
            notificacionesHabilitadas = notificaciones,
            temaOscuro = temaOscuro,
            idiomaApp = idiomaApp,
            perfilRemoto = remoto != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun onNombreChange(nombre: String) {
        if (profileRepository != null) return
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
