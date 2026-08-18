package ec.edu.uteq.scli.mobile.features.incidentes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidenteDao {

    @Query("SELECT * FROM incidentes ORDER BY creadoEnMillis DESC")
    fun observarTodos(): Flow<List<IncidenteEntity>>

    @Insert
    suspend fun insertar(incidente: IncidenteEntity): Long
}
