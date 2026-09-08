package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CatalogosRepository
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class NuevaReservaViewModel(
    private val repository: ReservaRepository,
    private val catalogos: CatalogosRepository? = null,
    private val perfilId: String = "",
    private val hoyProvider: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) },
) : ViewModel() {
    private val state = MutableStateFlow(NuevaReservaUiState())
    val uiState = state.asStateFlow()
    private var idempotencyKey: String? = null

    init {
        if (catalogos != null) {
            cargarCatalogos()
        } else {
            state.value = state.value.copy(
                cargandoCatalogos = false,
                rangoPeriodo = evaluarRangoPeriodo(state.value.periodo, hoyProvider()),
            )
        }
    }

    private fun cargarCatalogos() = viewModelScope.launch {
        state.value = try {
            val data = requireNotNull(catalogos).cargar(perfilId)
            val permitidas = data.horarios.map { it.materiaId }.toSet()
            val rango = evaluarRangoPeriodo(data.periodo, hoyProvider())
            state.value.copy(
                cargandoCatalogos = false,
                docenteId = data.docente.id,
                docenteCodigo = data.docente.codigoDocente,
                materias = data.materias.filter { it.activo && (permitidas.isEmpty() || it.id in permitidas) },
                laboratorios = data.laboratorios.filter { it.activo },
                periodo = data.periodo,
                rangoPeriodo = rango,
            )
        } catch (_: Exception) {
            state.value.copy(cargandoCatalogos = false, error = "No fue posible cargar los catálogos")
        }
    }

    fun actualizar(transform: (NuevaReservaUiState) -> NuevaReservaUiState) {
        if (state.value.enviando) return
        val anterior = state.value
        val nuevoEstado = transform(anterior)
        val fechaCambio = nuevoEstado.fechaReserva != anterior.fechaReserva
        val rango = evaluarRangoPeriodo(nuevoEstado.periodo, hoyProvider())
        state.value = nuevoEstado.copy(
            error = if (fechaCambio) null else nuevoEstado.error,
            disponible = if (fechaCambio) null else nuevoEstado.disponible,
            solicitudCreada = if (fechaCambio) null else nuevoEstado.solicitudCreada,
            rangoPeriodo = rango,
        )
        if (fechaCambio) {
            idempotencyKey = null
        }
    }

    fun actualizarFormulario(transform: (NuevaReservaUiState) -> NuevaReservaUiState) = actualizar(transform)

    fun comprobarDisponibilidad() {
        val s = state.value
        if (listOf(s.laboratorioId, s.fechaReserva, s.horaInicio, s.horaFin).any(String::isBlank)) return
        state.value = s.copy(comprobando = true, error = null)
        viewModelScope.launch {
            state.value = when (val result = repository.consultarDisponibilidad(s.laboratorioId, s.fechaReserva, s.horaInicio, s.horaFin)) {
                is NetworkResult.Success -> state.value.copy(comprobando = false, disponible = result.value.disponible)
                is NetworkResult.Failure -> state.value.copy(comprobando = false, error = mensaje(result))
            }
        }
    }

    fun enviar() {
        val s = state.value
        if (s.enviando) return

        if (s.periodo == null && s.periodoLectivoId.isBlank()) {
            val camposIncompletos = listOf(s.docenteId, s.materiaId, s.laboratorioId, s.fechaReserva, s.horaInicio, s.horaFin, s.motivo).any(String::isBlank) ||
                s.numeroParticipantes.toIntOrNull() == null || (s.numeroParticipantes.toIntOrNull() ?: 0) <= 0
            if (camposIncompletos && s.docenteId.isBlank() && s.materiaId.isBlank()) {
                state.value = s.copy(error = "Completa correctamente todos los campos obligatorios")
                return
            }
            state.value = s.copy(error = PeriodoReservaRango.SinPeriodo.mensaje)
            return
        }

        val rango = evaluarRangoPeriodo(s.periodo, hoyProvider())
        when (rango) {
            is PeriodoReservaRango.Invalido -> {
                state.value = s.copy(error = rango.mensaje)
                return
            }
            is PeriodoReservaRango.Valido -> {
                val fecha = parsearFechaSegura(s.fechaReserva)
                if (fecha == null || fecha.isBefore(rango.fechaMinima) || fecha.isAfter(rango.fechaMaxima)) {
                    state.value = s.copy(
                        error = "La fecha de la reserva debe estar comprendida entre ${rango.fechaMinima} y ${rango.fechaMaxima} para el período académico seleccionado."
                    )
                    return
                }
            }
        }

        val participantes = s.numeroParticipantes.toIntOrNull()
        val periodoId = s.periodo?.id ?: s.periodoLectivoId
        if (periodoId.isBlank() || participantes == null || participantes <= 0 ||
            listOf(s.docenteId, s.materiaId, s.laboratorioId, s.fechaReserva, s.horaInicio, s.horaFin, s.motivo).any(String::isBlank)) {
            state.value = s.copy(error = "Completa correctamente todos los campos obligatorios")
            return
        }

        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
        val request = NuevaSolicitudReserva(
            perfilId.ifBlank { s.solicitanteId },
            s.docenteId,
            s.laboratorioId,
            s.materiaId,
            periodoId,
            s.fechaReserva,
            s.horaInicio,
            s.horaFin,
            participantes,
            s.motivo,
            s.observacion.ifBlank { null }
        )
        state.value = s.copy(enviando = true, error = null)
        viewModelScope.launch {
            state.value = when (val result = repository.crearSolicitud(request, key)) {
                is NetworkResult.Success -> state.value.copy(enviando = false, solicitudCreada = result.value).also { idempotencyKey = null }
                is NetworkResult.Failure -> state.value.copy(enviando = false, error = mensaje(result))
            }
        }
    }

    private fun mensaje(error: NetworkResult.Failure) = when (error.statusCode) {
        400 -> error.message.takeIf { it.isNotBlank() && !it.startsWith("gateway_http_") }
            ?: "Datos de la solicitud inválidos."
        401 -> "Tu sesión expiró."
        403 -> "No tienes permisos para realizar esta acción."
        404 -> "No se encontró el recurso."
        409 -> "Existe un conflicto de horario o estado."
        else -> if (error.statusCode == null) "Sin conexión." else "No fue posible procesar la solicitud."
    }
}
