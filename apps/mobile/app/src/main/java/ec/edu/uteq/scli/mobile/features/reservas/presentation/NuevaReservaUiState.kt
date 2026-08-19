package ec.edu.uteq.scli.mobile.features.reservas.presentation

import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva

data class NuevaReservaUiState(
    val solicitanteId: String = "",
    val docenteId: String = "",
    val laboratorioId: String = "",
    val materiaId: String = "",
    val periodoLectivoId: String = "",
    val fechaReserva: String = "",
    val horaInicio: String = "",
    val horaFin: String = "",
    val numeroParticipantes: String = "",
    val motivo: String = "",
    val observacion: String = "",
    val enviando: Boolean = false,
    val error: String? = null,
    val solicitudCreada: SolicitudReserva? = null,
)
