package ec.edu.uteq.scli.mobile.features.incidentes.presentation

import ec.edu.uteq.scli.mobile.features.incidentes.domain.Incidente
import ec.edu.uteq.scli.mobile.features.incidentes.domain.Prioridad

data class IncidentesUiState(
    val incidentes: List<Incidente> = emptyList(),
    val laboratorioEquipo: String = "",
    val descripcion: String = "",
    val prioridad: Prioridad = Prioridad.MEDIA,
    val fechaMillis: Long = System.currentTimeMillis(),
    val error: String? = null,
)
