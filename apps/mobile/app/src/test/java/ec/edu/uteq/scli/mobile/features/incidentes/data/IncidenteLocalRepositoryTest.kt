package ec.edu.uteq.scli.mobile.features.incidentes.data

import ec.edu.uteq.scli.mobile.features.incidentes.domain.Incidente
import ec.edu.uteq.scli.mobile.features.incidentes.domain.Prioridad
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidenteLocalRepositoryTest {
    @Test
    fun `observarTodos mapea las entidades cacheadas al dominio`() = runTest {
        val dao = mockk<IncidenteDao>()
        val entidades = MutableStateFlow(
            listOf(
                IncidenteEntity(1, "Lab 1", "Sin energia", "ALTA", 100L, 200L),
            ),
        )
        every { dao.observarTodos() } returns entidades
        val repository = IncidenteLocalRepository(dao)

        val incidentes = repository.observarTodos().first()

        assertEquals(1, incidentes.size)
        assertEquals(Prioridad.ALTA, incidentes.single().prioridad)
        assertEquals("Lab 1", incidentes.single().laboratorioEquipo)
    }

    @Test
    fun `crear inserta la entidad y devuelve el incidente con el id generado`() = runTest {
        val dao = mockk<IncidenteDao>()
        coEvery { dao.insertar(any()) } returns 42L
        val repository = IncidenteLocalRepository(dao)
        val nuevo = Incidente(
            laboratorioEquipo = "Lab 2",
            descripcion = "Proyector dañado",
            prioridad = Prioridad.MEDIA,
            fechaMillis = 500L,
        )

        val creado = repository.crear(nuevo)

        assertEquals(42L, creado.id)
        assertEquals(nuevo.laboratorioEquipo, creado.laboratorioEquipo)
        coVerify { dao.insertar(match { it.prioridad == "MEDIA" && it.laboratorioEquipo == "Lab 2" }) }
    }
}
