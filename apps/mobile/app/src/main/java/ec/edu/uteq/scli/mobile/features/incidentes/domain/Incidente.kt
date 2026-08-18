package ec.edu.uteq.scli.mobile.features.incidentes.domain

/**
 * Modelo de dominio de un incidente, desacoplado de la entidad de persistencia
 * (Room) para que el ViewModel y la UI no dependan de detalles de storage.
 */
data class Incidente(
    val id: Long = 0,
    val laboratorioEquipo: String,
    val descripcion: String,
    val prioridad: Prioridad,
    val fechaMillis: Long,
    val creadoEnMillis: Long = System.currentTimeMillis(),
)
