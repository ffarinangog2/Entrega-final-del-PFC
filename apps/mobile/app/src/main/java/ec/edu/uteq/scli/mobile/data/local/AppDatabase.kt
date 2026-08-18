package ec.edu.uteq.scli.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ec.edu.uteq.scli.mobile.features.incidentes.data.IncidenteDao
import ec.edu.uteq.scli.mobile.features.incidentes.data.IncidenteEntity

@Database(
    entities = [IncidenteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incidenteDao(): IncidenteDao
}
