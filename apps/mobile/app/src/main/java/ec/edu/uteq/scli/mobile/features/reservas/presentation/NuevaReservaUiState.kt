package ec.edu.uteq.scli.mobile.features.reservas.presentation

import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.LaboratorioCatalogoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.MateriaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PeriodoDto

data class NuevaReservaUiState(
    val cargandoCatalogos: Boolean = true,
    val solicitanteId: String = "",
    val docenteId: String = "",
    val docenteCodigo: String = "",
    val materias: List<MateriaDto> = emptyList(),
    val laboratorios: List<LaboratorioCatalogoDto> = emptyList(),
    val periodo: PeriodoDto? = null,
    val laboratorioId: String = "",
    val materiaId: String = "",
    val periodoLectivoId: String = "",
    val fechaReserva: String = "",
    val horaInicio: String = "",
    val horaFin: String = "",
    val numeroParticipantes: String = "",
    val motivo: String = "",
    val observacion: String = "",
    val comprobando: Boolean = false,
    val disponible: Boolean? = null,
    val enviando: Boolean = false,
    val error: String? = null,
    val solicitudCreada: SolicitudReserva? = null,
)
