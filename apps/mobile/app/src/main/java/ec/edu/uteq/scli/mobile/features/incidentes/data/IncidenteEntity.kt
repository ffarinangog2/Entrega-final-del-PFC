package ec.edu.uteq.scli.mobile.features.incidentes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidentes")
data class IncidenteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val laboratorioEquipo: String,
    val descripcion: String,
    val prioridad: String,
    val fechaMillis: Long,
    val creadoEnMillis: Long,
)
