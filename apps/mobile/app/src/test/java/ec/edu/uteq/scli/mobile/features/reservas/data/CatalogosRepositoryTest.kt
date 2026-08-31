package ec.edu.uteq.scli.mobile.features.reservas.data

import ec.edu.uteq.scli.mobile.features.reservas.data.remote.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogosRepositoryTest {
    @Test fun `perfil resuelve docente real y carga catalogos humanos`() = runTest {
        val api = mockk<CatalogosApi>()
        coEvery { api.docentePorPerfil("perfil-1") } returns DocenteDto("docente-real", "perfil-1", "DOC-001", true)
        coEvery { api.materias(0, 100) } returns pagina(listOf(MateriaDto("m1", "MAT-1", "Redes", true)))
        coEvery { api.periodoActual() } returns PeriodoDto("p1", "2026-A", "2026 A", EstadoPeriodoDto.ACTIVO)
        coEvery { api.laboratorios(0, 100) } returns pagina(listOf(LaboratorioCatalogoDto("l1", "LAB-1", "Redes", "piso", true)))
        coEvery { api.horarios("docente-real") } returns listOf(HorarioDto("h1", "docente-real", "m1", "p1", "l1"))
        val result = CatalogosRepository(api).cargar("perfil-1")
        assertEquals("docente-real", result.docente.id)
        assertEquals("Redes", result.materias.single().nombre)
        coVerify { api.horarios("docente-real") }
    }

    @Test fun `recorre las paginas declaradas sin solicitar una pagina inexistente`() = runTest {
        val api = mockk<CatalogosApi>()
        coEvery { api.laboratorios(0, 100) } returns pagina(
            listOf(LaboratorioCatalogoDto("l1", "LAB-1", "Uno", null, true)),
            number = 0, totalPages = 2, last = false,
        )
        coEvery { api.laboratorios(1, 100) } returns pagina(
            listOf(LaboratorioCatalogoDto("l2", "LAB-2", "Dos", null, true)),
            number = 1, totalPages = 2, last = true,
        )

        val result = CatalogosRepository(api).laboratorios()

        assertEquals(listOf("l1", "l2"), result.map { it.id })
        coVerify(exactly = 1) { api.laboratorios(0, 100) }
        coVerify(exactly = 1) { api.laboratorios(1, 100) }
    }

    private fun <T> pagina(
        content: List<T>,
        number: Int = 0,
        totalPages: Int = 1,
        last: Boolean = true,
    ) = PageResponse(
        content = content,
        number = number,
        size = 100,
        totalElements = content.size.toLong(),
        totalPages = totalPages,
        numberOfElements = content.size,
        first = number == 0,
        last = last,
        empty = content.isEmpty(),
    )
}
