package ec.edu.uteq.scli.mobile.features.reservas.presentation

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CatalogosRepository
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CatalogosSolicitud
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CrearSolicitudReservaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.DocenteDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.EstadoPeriodoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.HorarioDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.LaboratorioCatalogoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.MateriaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PeriodoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PisoCatalogoDto
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Disponibilidad
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Pagina
import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NuevaReservaPisoFiltroTest {

    private val dispatcher = StandardTestDispatcher()
    private val hoyFijo = LocalDate.of(2026, 8, 10)

    private val pisoPB = PisoCatalogoDto("piso-0", 0, "Planta Baja", activo = true)
    private val piso1 = PisoCatalogoDto("piso-1", 1, "Bloque A", activo = true)
    private val piso2 = PisoCatalogoDto("piso-2", 2, "Segundo Piso", activo = true)
    private val pisoVacio = PisoCatalogoDto("piso-vacio", 3, "Piso Sin Labs", activo = true)

    private val labPB1 = LaboratorioCatalogoDto("lab-pb-1", "LAB-PB-01", "Lab PB", "piso-0", activo = true)
    private val lab1A = LaboratorioCatalogoDto("lab-1a", "LAB-101", "Lab Redes", "piso-1", activo = true)
    private val lab1B = LaboratorioCatalogoDto("lab-1b", "LAB-102", "Lab Software", "piso-1", activo = true)
    private val lab2A = LaboratorioCatalogoDto("lab-2a", "LAB-201", "Lab Fisica", "piso-2", activo = true)
    private val labSinPiso = LaboratorioCatalogoDto("lab-sin-piso", "LAB-XXX", "Lab Huerfano", null, activo = true)

    private val periodoActivo = PeriodoDto(
        id = "periodo-1",
        codigo = "2026-A",
        nombre = "Periodo 2026-A",
        estado = EstadoPeriodoDto.ACTIVO,
        fechaInicio = "2026-08-01",
        fechaFin = "2026-12-31"
    )

    private lateinit var testRepo: TestReservaRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        testRepo = TestReservaRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModelConCatalogos(
        pisos: List<PisoCatalogoDto> = listOf(piso2, pisoPB, piso1, pisoVacio),
        labs: List<LaboratorioCatalogoDto> = listOf(labPB1, lab1A, lab1B, lab2A, labSinPiso),
    ): Pair<NuevaReservaViewModel, CatalogosRepository> {
        val catalogos = mockk<CatalogosRepository>()
        coEvery { catalogos.cargar("perfil-1") } returns CatalogosSolicitud(
            docente = DocenteDto("docente-1", "perfil-1", "DOC-001", true),
            materias = listOf(MateriaDto("mat-1", "MAT-01", "Sistemas", true)),
            periodo = periodoActivo,
            laboratorios = labs,
            horarios = emptyList(),
            pisos = pisos,
        )
        val vm = NuevaReservaViewModel(
            repository = testRepo,
            catalogos = catalogos,
            perfilId = "perfil-1",
            hoyProvider = { hoyFijo }
        )
        return vm to catalogos
    }

    @Test
    fun `A - pisos se cargan y ordenan por numero ascendente`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(4, state.pisos.size)
        assertEquals(listOf(0, 1, 2, 3), state.pisos.map { it.numero })
        assertEquals(listOf("piso-0", "piso-1", "piso-2", "piso-vacio"), state.pisos.map { it.id })
    }

    @Test
    fun `B - piso 0 produce Piso 1 · Planta Baja`() {
        val label1 = formatPisoLabel(PisoCatalogoDto("p0", 0, "Planta Baja"))
        val label2 = formatPisoLabel(PisoCatalogoDto("p0", 0, null))
        val label3 = formatPisoLabel(PisoCatalogoDto("p0", 0, ""))

        assertEquals("Piso 1 · Planta Baja", label1)
        assertEquals("Piso 1 · Planta Baja", label2)
        assertEquals("Piso 1 · Planta Baja", label3)
    }

    @Test
    fun `C - piso 2 produce Piso 3 y descripciones informativas no duplicadas se anexan`() {
        val labelSinDesc = formatPisoLabel(PisoCatalogoDto("p2", 2, null))
        val labelDescUtil = formatPisoLabel(PisoCatalogoDto("p1", 1, "Bloque A"))
        val labelDescDuplicada = formatPisoLabel(PisoCatalogoDto("p1", 1, "Piso 2"))

        assertEquals("Piso 3", labelSinDesc)
        assertEquals("Piso 2 — Bloque A", labelDescUtil)
        assertEquals("Piso 2", labelDescDuplicada)
    }

    @Test
    fun `D - seleccionar Piso 1 muestra solo laboratorios del Piso 1`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")

        val state = vm.uiState.value
        assertEquals("piso-1", state.pisoId)
        assertEquals(listOf("lab-1a", "lab-1b"), state.laboratoriosFiltrados.map { it.id })
    }

    @Test
    fun `E - laboratorio de Piso 2 no aparece al seleccionar Piso 1`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")

        val filtrados = vm.uiState.value.laboratoriosFiltrados
        assertFalse(filtrados.any { it.id == "lab-2a" })
        assertFalse(filtrados.any { it.pisoId == "piso-2" })
    }

    @Test
    fun `F - laboratorio con pisoId null no provoca crash y no aparece en ningun piso`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-0")
        assertFalse(vm.uiState.value.laboratoriosFiltrados.any { it.id == "lab-sin-piso" })

        vm.seleccionarPiso("piso-1")
        assertFalse(vm.uiState.value.laboratoriosFiltrados.any { it.id == "lab-sin-piso" })

        vm.seleccionarPiso("piso-2")
        assertFalse(vm.uiState.value.laboratoriosFiltrados.any { it.id == "lab-sin-piso" })
    }

    @Test
    fun `G - cambiar de Piso 1 a Piso 2 limpia laboratorio incompatible`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")
        vm.seleccionarLaboratorio("lab-1a")
        assertEquals("lab-1a", vm.uiState.value.laboratorioId)

        // Cambiar a Piso 2
        vm.seleccionarPiso("piso-2")
        assertEquals("", vm.uiState.value.laboratorioId)
    }

    @Test
    fun `H - cambiar de piso invalida disponibilidad previa`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")
        vm.seleccionarLaboratorio("lab-1a")
        vm.actualizar {
            it.copy(
                fechaReserva = "2026-08-20",
                horaInicio = "08:00",
                horaFin = "10:00"
            )
        }
        vm.comprobarDisponibilidad()
        runCurrent()

        assertEquals(true, vm.uiState.value.disponible)

        // Cambiar piso debe invalidar disponibilidad
        vm.seleccionarPiso("piso-2")
        assertNull(vm.uiState.value.disponible)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `I - cambiar de laboratorio dentro del mismo piso invalida disponibilidad previa`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")
        vm.seleccionarLaboratorio("lab-1a")
        vm.actualizar {
            it.copy(
                fechaReserva = "2026-08-20",
                horaInicio = "08:00",
                horaFin = "10:00"
            )
        }
        vm.comprobarDisponibilidad()
        runCurrent()

        assertEquals(true, vm.uiState.value.disponible)

        // Cambiar a otro laboratorio del mismo piso
        vm.seleccionarLaboratorio("lab-1b")
        assertNull(vm.uiState.value.disponible)
        assertNull(vm.uiState.value.error)
        assertEquals("lab-1b", vm.uiState.value.laboratorioId)
    }

    @Test
    fun `J - seleccionar exactamente el mismo piso conserva laboratorio y no resetea innecesariamente`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")
        vm.seleccionarLaboratorio("lab-1a")
        vm.actualizar {
            it.copy(
                fechaReserva = "2026-08-20",
                horaInicio = "08:00",
                horaFin = "10:00"
            )
        }
        vm.comprobarDisponibilidad()
        runCurrent()

        assertEquals(true, vm.uiState.value.disponible)

        // Re-seleccionar el mismo piso
        vm.seleccionarPiso("piso-1")
        assertEquals("lab-1a", vm.uiState.value.laboratorioId)
        assertEquals(true, vm.uiState.value.disponible)
    }

    @Test
    fun `K - piso sin laboratorios produce lista vacia`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-vacio")
        assertTrue(vm.uiState.value.laboratoriosFiltrados.isEmpty())
        assertEquals("", vm.uiState.value.laboratorioId)
    }

    @Test
    fun `L - comprobar disponibilidad con laboratorio incompatible NO llama repository`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")
        runCurrent()

        // Estado inconsistente: piso 1 pero laboratorio del piso 2 (forzado directamente sin cambio de piso)
        vm.actualizar {
            it.copy(
                laboratorioId = "lab-2a",
                fechaReserva = "2026-08-20",
                horaInicio = "08:00",
                horaFin = "10:00"
            )
        }

        vm.comprobarDisponibilidad()
        runCurrent()

        assertEquals(0, testRepo.consultasDisponibilidad)
        assertEquals("El laboratorio seleccionado no pertenece al piso.", vm.uiState.value.error)
    }

    @Test
    fun `M - enviar con laboratorio incompatible NO llama repository`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")
        runCurrent()

        // Estado inconsistente: piso 1 pero laboratorio del piso 2 (forzado directamente sin cambio de piso)
        vm.actualizar {
            it.copy(
                materiaId = "mat-1",
                laboratorioId = "lab-2a",
                fechaReserva = "2026-08-20",
                horaInicio = "08:00",
                horaFin = "10:00",
                numeroParticipantes = "20",
                motivo = "Clase de prueba"
            )
        }

        vm.enviar()
        runCurrent()

        assertEquals(0, testRepo.creaciones)
        assertEquals("El laboratorio seleccionado no pertenece al piso.", vm.uiState.value.error)
    }

    @Test
    fun `N - solicitud valida contiene laboratorioId correcto al enviar`() = runTest {
        val (vm, _) = crearViewModelConCatalogos()
        runCurrent()

        vm.seleccionarPiso("piso-1")
        vm.seleccionarLaboratorio("lab-1a")
        vm.actualizar {
            it.copy(
                materiaId = "mat-1",
                fechaReserva = "2026-08-20",
                horaInicio = "08:00",
                horaFin = "10:00",
                numeroParticipantes = "25",
                motivo = "Laboratorio de Redes"
            )
        }

        vm.enviar()
        runCurrent()

        assertEquals(1, testRepo.creaciones)
        assertEquals("lab-1a", testRepo.ultimaSolicitud?.laboratorioId)
        assertEquals("mat-1", testRepo.ultimaSolicitud?.materiaId)
        assertEquals("docente-1", testRepo.ultimaSolicitud?.docenteId)
    }

    @Test
    fun `O - modelos de solicitud hacia backend NO contienen pisoId`() {
        val camposNuevaSolicitud = NuevaSolicitudReserva::class.java.declaredFields.map { it.name }
        assertFalse("NuevaSolicitudReserva no debe contener pisoId", camposNuevaSolicitud.any { it.contains("piso", ignoreCase = true) })

        val camposCrearDto = CrearSolicitudReservaDto::class.java.declaredFields.map { it.name }
        assertFalse("CrearSolicitudReservaDto no debe contener pisoId", camposCrearDto.any { it.contains("piso", ignoreCase = true) })
    }

    @Test
    fun `P - alternar pisos no dispara nuevas llamadas HTTP al catalogo o backend`() = runTest {
        val (vm, catalogos) = crearViewModelConCatalogos()
        runCurrent()

        coVerify(exactly = 1) { catalogos.cargar("perfil-1") }

        // Alternar pisos multiples veces
        vm.seleccionarPiso("piso-0")
        vm.seleccionarPiso("piso-1")
        vm.seleccionarPiso("piso-2")
        vm.seleccionarPiso("piso-0")

        // Verificar que no hubo ninguna llamada adicional
        coVerify(exactly = 1) { catalogos.cargar(any()) }
        assertEquals(0, testRepo.consultasDisponibilidad)
        assertEquals(0, testRepo.creaciones)
    }

    private class TestReservaRepository : ReservaRepository {
        var consultasDisponibilidad = 0
        var creaciones = 0
        var ultimaSolicitud: NuevaSolicitudReserva? = null

        override suspend fun consultarDisponibilidad(
            laboratorioId: String,
            fecha: String,
            horaInicio: String,
            horaFin: String
        ): NetworkResult<Disponibilidad> {
            consultasDisponibilidad++
            return NetworkResult.Success(Disponibilidad(laboratorioId, fecha, horaInicio, horaFin, true, null))
        }

        override suspend fun crearSolicitud(
            solicitud: NuevaSolicitudReserva,
            idempotencyKey: String
        ): NetworkResult<SolicitudReserva> {
            creaciones++
            ultimaSolicitud = solicitud
            return NetworkResult.Success(
                SolicitudReserva(
                    id = "sol-1",
                    solicitanteId = solicitud.solicitanteId,
                    docenteId = solicitud.docenteId,
                    laboratorioId = solicitud.laboratorioId,
                    materiaId = solicitud.materiaId,
                    periodoLectivoId = solicitud.periodoLectivoId,
                    fechaReserva = solicitud.fechaReserva,
                    horaInicio = solicitud.horaInicio,
                    horaFin = solicitud.horaFin,
                    numeroParticipantes = solicitud.numeroParticipantes,
                    motivo = solicitud.motivo,
                    observacion = solicitud.observacion,
                    estado = "PENDIENTE",
                    reservaId = null,
                    creadaEn = "2026-08-10T10:00:00Z",
                    actualizadaEn = "2026-08-10T10:00:00Z",
                    version = 1
                )
            )
        }

        override suspend fun listar(pagina: Int, tamanio: Int): NetworkResult<Pagina<Reserva>> =
            NetworkResult.Failure(null, "no_usado")

        override suspend fun obtener(id: String): NetworkResult<Reserva> =
            NetworkResult.Failure(null, "no_usado")

        override suspend fun actualizarSolicitud(
            id: String,
            solicitud: ActualizacionSolicitudReserva
        ): NetworkResult<SolicitudReserva> = NetworkResult.Failure(null, "no_usado")

        override suspend fun cancelarSolicitud(id: String, comentario: String): NetworkResult<SolicitudReserva> =
            NetworkResult.Failure(null, "no_usado")

        override suspend fun cancelarReserva(id: String, motivo: String): NetworkResult<Reserva> =
            NetworkResult.Failure(null, "no_usado")
    }
}
