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
        coEvery { api.pisos(0, 100, true) } returns pagina(listOf(PisoCatalogoDto("piso-1", 1, "Piso 2", true)))
        val result = CatalogosRepository(api).cargar("perfil-1")
        assertEquals("docente-real", result.docente.id)
        assertEquals("Redes", result.materias.single().nombre)
        assertEquals("piso-1", result.pisos.single().id)
        coVerify { api.horarios("docente-real") }
        coVerify { api.pisos(0, 100, true) }
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

    @Test fun `pisos recorre paginas y los ordena por numero ascendente`() = runTest {
        val api = mockk<CatalogosApi>()
        coEvery { api.pisos(0, 100, true) } returns pagina(
            listOf(
                PisoCatalogoDto("piso-2", 2, "Piso 3", true),
                PisoCatalogoDto("piso-0", 0, "Planta Baja", true),
                PisoCatalogoDto("piso-inactivo", 1, "Inactivo", false),
            ),
            number = 0, totalPages = 1, last = true,
        )

        val result = CatalogosRepository(api).pisos()

        assertEquals(listOf("piso-0", "piso-2"), result.map { it.id })
        assertEquals(listOf(0, 2), result.map { it.numero })
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
