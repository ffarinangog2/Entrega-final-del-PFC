package ec.edu.uteq.scli.mobile.features.reservas.presentation

import ec.edu.uteq.scli.mobile.features.reservas.data.remote.LaboratorioCatalogoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.MateriaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PeriodoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PisoCatalogoDto
import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva

fun formatPisoLabel(piso: PisoCatalogoDto): String {
    val base = if (piso.numero == 0) "Piso 1 · Planta Baja" else "Piso ${piso.numero + 1}"
    val desc = piso.descripcion?.trim().orEmpty()
    val descDuplica = desc.isBlank() ||
        desc.equals("Planta Baja", ignoreCase = true) ||
        desc.equals(base, ignoreCase = true) ||
        desc.equals("Piso ${piso.numero}", ignoreCase = true) ||
        desc.equals("Piso ${piso.numero + 1}", ignoreCase = true)
    return if (!descDuplica) "$base — $desc" else base
}

data class NuevaReservaUiState(
    val cargandoCatalogos: Boolean = true,
    val solicitanteId: String = "",
    val docenteId: String = "",
    val docenteCodigo: String = "",
    val materias: List<MateriaDto> = emptyList(),
    val pisos: List<PisoCatalogoDto> = emptyList(),
    val laboratorios: List<LaboratorioCatalogoDto> = emptyList(),
    val periodo: PeriodoDto? = null,
    val pisoId: String = "",
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
    val rangoPeriodo: PeriodoReservaRango = PeriodoReservaRango.SinPeriodo,
) {
    val laboratoriosFiltrados: List<LaboratorioCatalogoDto>
        get() = if (pisoId.isBlank()) emptyList() else laboratorios.filter { it.pisoId == pisoId }
}
