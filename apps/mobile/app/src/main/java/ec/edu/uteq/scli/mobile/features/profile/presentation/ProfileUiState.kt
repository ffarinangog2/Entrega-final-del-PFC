package ec.edu.uteq.scli.mobile.features.profile.presentation

data class ProfileUiState(
    val nombreUsuario: String = "",
    val notificacionesHabilitadas: Boolean = true,
    val temaOscuro: Boolean? = null,
    val idiomaApp: String? = null,
    val perfilRemoto: Boolean = false,
)
