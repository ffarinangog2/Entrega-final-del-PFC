package ec.edu.uteq.scli.mobile.features.reservas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReservaDao {
    @Query("SELECT * FROM reservas_cache ORDER BY fechaReserva, horaInicio")
    suspend fun obtenerTodas(): List<ReservaEntity>

    @Query("SELECT * FROM reservas_cache WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): ReservaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodas(reservas: List<ReservaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(reserva: ReservaEntity)
}
