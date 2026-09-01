package ec.edu.uteq.scli.mobile.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.features.profile.data.SettingsRepository
import ec.edu.uteq.scli.mobile.features.profile.data.ProfileRepository
import ec.edu.uteq.scli.mobile.features.profile.data.ProfileDto
import ec.edu.uteq.scli.mobile.features.profile.data.UpdateOwnProfileRequest
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
    private val perfil = MutableStateFlow<ProfileDto?>(null)
    private val formulario = MutableStateFlow(UpdateOwnProfileRequest(null, null, null, null))
    private val guardando = MutableStateFlow(false)
    private val mensaje = MutableStateFlow<String?>(null)

    init {
        if (profileRepository != null) viewModelScope.launch {
            runCatching { profileRepository.getOwnProfile() }
                .onSuccess {
                    perfil.value = it
                    formulario.value = UpdateOwnProfileRequest(it.emailPersonal, it.telefono, it.direccion, it.fotoUrl)
                    nombreRemoto.value = "${it.nombres} ${it.apellidos}".trim()
                }
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.nombreUsuario,
        settingsRepository.notificacionesHabilitadas,
        settingsRepository.temaOscuro,
        settingsRepository.idiomaApp,
        nombreRemoto,
        perfil,
        formulario,
        guardando,
        mensaje,
    ) { values ->
        val nombre = values[0] as String
        val notificaciones = values[1] as Boolean
        val temaOscuro = values[2] as Boolean?
        val idiomaApp = values[3] as String?
        val remoto = values[4] as String?
        val perfilActual = values[5] as ProfileDto?
        val form = values[6] as UpdateOwnProfileRequest
        ProfileUiState(
            nombreUsuario = remoto ?: nombre,
            notificacionesHabilitadas = notificaciones,
            temaOscuro = temaOscuro,
            idiomaApp = idiomaApp,
            perfilRemoto = remoto != null,
            emailInstitucional = perfilActual?.emailInstitucional.orEmpty(),
            emailPersonal = form.emailPersonal.orEmpty(),
            telefono = form.telefono.orEmpty(),
            direccion = form.direccion.orEmpty(),
            fotoUrl = form.fotoUrl.orEmpty(),
            guardando = values[7] as Boolean,
            mensaje = values[8] as String?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun onNombreChange(nombre: String) {
        if (profileRepository != null) return
        viewModelScope.launch {
            settingsRepository.setNombreUsuario(nombre)
            Timber.tag("ProfileViewModel").d("Nombre del usuario actualizado")
        }
    }

    fun onEmailPersonalChange(value: String) { formulario.value = formulario.value.copy(emailPersonal = value) }
    fun onTelefonoChange(value: String) { formulario.value = formulario.value.copy(telefono = value) }
    fun onDireccionChange(value: String) { formulario.value = formulario.value.copy(direccion = value) }
    fun onFotoUrlChange(value: String) { formulario.value = formulario.value.copy(fotoUrl = value) }

    fun guardarPerfil() {
        val repository = profileRepository ?: return
        viewModelScope.launch {
            guardando.value = true
            mensaje.value = null
            runCatching { repository.updateOwnProfile(formulario.value) }
                .onSuccess { perfil.value = it; mensaje.value = "Perfil actualizado" }
                .onFailure { mensaje.value = "No fue posible actualizar el perfil" }
            guardando.value = false
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
