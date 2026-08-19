package ec.edu.uteq.scli.mobile.features.reservas.presentation

import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva

data class ReservasUiState(
    val reservas: List<Reserva> = emptyList(),
    val seleccionada: Reserva? = null,
    val cargando: Boolean = false,
    val refrescando: Boolean = false,
    val error: String? = null,
    val cancelando: Boolean = false,
    val cancelacionExitosa: Boolean = false,
    val desdeCache: Boolean = false,
    val errorActualizacion: String? = null,
)
