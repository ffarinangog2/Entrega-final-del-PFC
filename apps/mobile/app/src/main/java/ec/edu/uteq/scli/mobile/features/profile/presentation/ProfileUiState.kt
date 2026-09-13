package ec.edu.uteq.scli.mobile.features.profile.presentation

data class ProfileUiState(
    val nombreUsuario: String = "",
    val notificacionesHabilitadas: Boolean = true,
    val temaOscuro: Boolean? = null,
    val idiomaApp: String? = null,
    val perfilRemoto: Boolean = false,
    val emailInstitucional: String = "",
    val emailPersonal: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val fotoUrl: String = "",
    val guardando: Boolean = false,
    val mensaje: String? = null,
)
