package ec.edu.uteq.scli.mobile.features.reservas

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ec.edu.uteq.scli.mobile.data.local.AppDatabase
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-reservas-test.db"

    @Before fun setUp() = context.deleteDatabase(databaseName).let { Unit }
    @After fun tearDown() = context.deleteDatabase(databaseName).let { Unit }

    @Test
    fun migracionUnoADosPreservaIncidentesYHabilitaCacheReservas() = runTest {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """CREATE TABLE IF NOT EXISTS `incidentes` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `laboratorioEquipo` TEXT NOT NULL, `descripcion` TEXT NOT NULL,
                                `prioridad` TEXT NOT NULL, `fechaMillis` INTEGER NOT NULL,
                                `creadoEnMillis` INTEGER NOT NULL)""".trimIndent(),
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        )
        helper.writableDatabase.execSQL(
            "INSERT INTO incidentes (laboratorioEquipo, descripcion, prioridad, fechaMillis, creadoEnMillis) " +
                "VALUES ('Lab 1', 'Sin red', 'MEDIA', 1, 2)",
        )
        helper.close()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        try {
            val incidentes = database.openHelper.readableDatabase.query("SELECT * FROM incidentes")
            incidentes.use {
                assertTrue(it.moveToFirst())
                assertEquals("Sin red", it.getString(it.getColumnIndexOrThrow("descripcion")))
            }

            val reserva = ReservaEntity(
                "reserva-1", "solicitud-1", "laboratorio-1", "responsable-1", "2026-08-20",
                "08:00:00", "10:00:00", "PROGRAMADA", "RES-001",
                "2026-08-18T10:00:00Z", "2026-08-18T10:00:00Z", 0,
            )
            database.reservaDao().guardar(reserva)
            assertEquals(reserva, database.reservaDao().obtenerPorId(reserva.id))
        } finally {
            database.close()
        }
    }
}
