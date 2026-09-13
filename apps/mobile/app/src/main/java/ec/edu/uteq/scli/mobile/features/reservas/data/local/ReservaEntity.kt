package ec.edu.uteq.scli.mobile.features.reservas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservas_cache")
data class ReservaEntity(
    @PrimaryKey val id: String,
    val solicitudId: String,
    val laboratorioId: String,
    val responsableId: String,
    val fechaReserva: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: String,
    val codigoReserva: String,
    val creadaEn: String,
    val actualizadaEn: String,
    val version: Long,
)
