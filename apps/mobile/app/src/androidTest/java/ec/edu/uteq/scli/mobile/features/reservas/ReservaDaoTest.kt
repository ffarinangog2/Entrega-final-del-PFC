package ec.edu.uteq.scli.mobile.features.reservas

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ec.edu.uteq.scli.mobile.data.local.AppDatabase
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReservaDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() = database.close()

    @Test
    fun guardaListadoYActualizaDetalle() = runTest {
        val inicial = reserva("PROGRAMADA", 0)
        database.reservaDao().guardarTodas(listOf(inicial))
        assertEquals(listOf(inicial), database.reservaDao().obtenerTodas())

        val actualizada = reserva("CANCELADA", 1)
        database.reservaDao().guardar(actualizada)
        assertEquals(actualizada, database.reservaDao().obtenerPorId(inicial.id))
    }

    private fun reserva(estado: String, version: Long) = ReservaEntity(
        "reserva-1", "solicitud-1", "laboratorio-1", "responsable-1", "2026-08-20",
        "08:00:00", "10:00:00", estado, "RES-001",
        "2026-08-18T10:00:00Z", "2026-08-18T10:00:00Z", version,
    )
}
