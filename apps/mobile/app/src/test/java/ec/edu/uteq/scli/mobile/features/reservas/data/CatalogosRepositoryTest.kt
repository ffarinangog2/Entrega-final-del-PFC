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
        coEvery { api.materias() } returns listOf(MateriaDto("m1", "MAT-1", "Redes", true))
        coEvery { api.periodoActual() } returns PeriodoDto("p1", "2026-A", "2026 A", true)
        coEvery { api.laboratorios() } returns listOf(LaboratorioCatalogoDto("l1", "LAB-1", "Redes", "piso", true))
        coEvery { api.horarios("docente-real") } returns listOf(HorarioDto("h1", "docente-real", "m1", "p1", "l1"))
        val result = CatalogosRepository(api).cargar("perfil-1")
        assertEquals("docente-real", result.docente.id)
        assertEquals("Redes", result.materias.single().nombre)
        coVerify { api.horarios("docente-real") }
    }
}
