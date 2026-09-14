package ec.edu.uteq.scli.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ec.edu.uteq.scli.mobile.features.incidentes.data.IncidenteDao
import ec.edu.uteq.scli.mobile.features.incidentes.data.IncidenteEntity
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaDao
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaEntity

@Database(
    entities = [IncidenteEntity::class, ReservaEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incidenteDao(): IncidenteDao
    abstract fun reservaDao(): ReservaDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `reservas_cache` (
                        `id` TEXT NOT NULL, `solicitudId` TEXT NOT NULL,
                        `laboratorioId` TEXT NOT NULL, `responsableId` TEXT NOT NULL,
                        `fechaReserva` TEXT NOT NULL, `horaInicio` TEXT NOT NULL,
                        `horaFin` TEXT NOT NULL, `estado` TEXT NOT NULL,
                        `codigoReserva` TEXT NOT NULL, `creadaEn` TEXT NOT NULL,
                        `actualizadaEn` TEXT NOT NULL, `version` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))""".trimIndent(),
                )
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE incidentes ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE incidentes ADD COLUMN estado TEXT NOT NULL DEFAULT 'REPORTADO'")
            }
        }
    }
}
