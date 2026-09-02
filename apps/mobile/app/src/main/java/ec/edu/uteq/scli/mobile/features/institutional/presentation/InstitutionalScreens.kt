package ec.edu.uteq.scli.mobile.features.institutional.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.features.institutional.data.CoordinacionData
import ec.edu.uteq.scli.mobile.features.institutional.data.PlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PeriodoPlanificacionDto

@Composable
fun PlanificacionesScreen(
    viewModel: InstitutionalViewModel,
    puedeRevisar: Boolean,
    coordinador: Boolean,
    administradorPiso: Boolean = false,
) {
    if (coordinador) {
        CoordinacionScreen(viewModel)
        return
    }
    if (administradorPiso) {
        AdministradorPisoPlanificacionScreen(viewModel)
        return
    }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarPlanificaciones() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Planificación semestral") }
        if (state.cargando) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it) } }
        state.mensaje?.let { item { Text(it) } }
        if (!state.cargando && state.planificaciones.isEmpty()) item { Text("No hay planificaciones para tu ámbito") }
        items(state.planificaciones, key = { it.id }) { plan ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${plan.diaSemana} ${plan.horaInicio}–${plan.horaFin}")
                Text("Estado: ${plan.estado.replace('_', ' ')}")
                plan.observacion?.let { Text(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (puedeRevisar && plan.estado == "ENVIADA") {
                        Button(onClick = { viewModel.aceptar(plan.id) }) { Text("Aceptar") }
                        OutlinedButton(onClick = { viewModel.rechazar(plan.id, "Rechazada desde Mobile") }) { Text("Rechazar") }
                    }
                    if (coordinador && plan.estado == "PROPUESTA_CAMBIO") {
                        Button(onClick = { viewModel.aceptarPropuesta(plan.id) }) { Text("Aceptar propuesta") }
                    }
                }
            }
        }
    }
}

@Composable
fun AdministradorPisoPlanificacionScreen(viewModel: InstitutionalViewModel) {
    val state by viewModel.uiState.collectAsState()
    var dia by remember { mutableStateOf("LUNES") }
    var motivoRechazo by remember { mutableStateOf("") }
    var planObservado by remember { mutableStateOf<PlanificacionDto?>(null) }
    var observacion by remember { mutableStateOf("") }
    var confirmarAprobacion by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.cargarCoordinacion() }
    val data = state.coordinacion
    val pendientes = state.planificaciones.filter { it.estado == "ENVIADA" }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Administración de piso", style = MaterialTheme.typography.headlineMedium)
            Text("Planificación recibida como conjunto")
        }
        if (state.cargando && data == null) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        state.mensaje?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        data?.let { paquete ->
            item {
                ResumenCoordinacion(
                    paquete,
                    paquete.periodo,
                    paquete.planificacion?.estado,
                    paquete.planificaciones.size,
                )
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    diasPlanificacion.forEach { value ->
                        FilterChip(selected = dia == value, onClick = { dia = value }, label = { Text(etiquetaDia(value)) })
                    }
                }
            }
            val bloques = paquete.planificaciones.filter { it.diaSemana == dia && it.estado != "CANCELADA" }.sortedBy { it.horaInicio }
            if (bloques.isEmpty()) item { Text("No hay bloques para ${etiquetaDia(dia)}.") }
            items(bloques, key = { it.id }) { plan ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${plan.horaInicio} - ${plan.horaFin}", style = MaterialTheme.typography.titleMedium)
                        Text(paquete.materias.find { it.id == plan.materiaId }?.nombre ?: "Materia asignada")
                        Text(paquete.laboratorios.find { it.id == plan.laboratorioId }?.codigo ?: "Laboratorio")
                        Text("Docente asignado")
                        Text("Estado: ${etiquetaEstado(plan.estado)}")
                        plan.observacion?.let { Text("Observación: $it") }
                        if (plan.estado == "ENVIADA") OutlinedButton(onClick = { planObservado = plan }) { Text("Marcar bloque problemático") }
                    }
                }
            }
            if (pendientes.isNotEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirmarAprobacion = true }, enabled = !state.cargando, modifier = Modifier.fillMaxWidth()) { Text("Aprobar planificación") }
                    OutlinedTextField(motivoRechazo, { motivoRechazo = it }, label = { Text("Motivo del rechazo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { viewModel.rechazarPaquete(motivoRechazo) }, enabled = motivoRechazo.isNotBlank() && !state.cargando, modifier = Modifier.fillMaxWidth()) { Text("Rechazar planificación") }
                }
            }
            item { Text("Laboratorios de mi piso", style = MaterialTheme.typography.titleLarge) }
            if (paquete.laboratorios.isEmpty()) item { Text("No hay laboratorios disponibles en su ámbito.") }
            items(paquete.laboratorios, key = { "lab-${it.id}" }) { laboratorio ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(laboratorio.codigo, style = MaterialTheme.typography.titleMedium)
                        Text(laboratorio.nombre)
                        Text(estadoLaboratorio(laboratorio.estado))
                    }
                }
            }
        }
    }
    if (confirmarAprobacion) AlertDialog(
        onDismissRequest = { confirmarAprobacion = false },
        title = { Text("Aprobar planificación") },
        text = { Text("¿Desea aprobar todos los bloques pendientes de esta planificación?") },
        confirmButton = { TextButton(onClick = { confirmarAprobacion = false; viewModel.aprobarPaquete() }) { Text("Aprobar") } },
        dismissButton = { TextButton(onClick = { confirmarAprobacion = false }) { Text("Cancelar") } },
    )
    planObservado?.let { plan -> AlertDialog(
        onDismissRequest = { planObservado = null },
        title = { Text("Observación del bloque") },
        text = { OutlinedTextField(observacion, { observacion = it }, label = { Text("Problema o propuesta") }) },
        confirmButton = { TextButton(enabled = observacion.isNotBlank(), onClick = { viewModel.proponerCambio(plan.id, observacion); observacion = ""; planObservado = null }) { Text("Enviar") } },
        dismissButton = { TextButton(onClick = { planObservado = null }) { Text("Cancelar") } },
    ) }
}

private val diasPlanificacion = listOf("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES")

@Composable
fun CoordinacionScreen(viewModel: InstitutionalViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarCoordinacion() }
    CoordinacionContent(state)
}

@Composable
internal fun CoordinacionContent(state: InstitutionalUiState) {
    var diaSeleccionado by remember { mutableStateOf("LUNES") }
    var nivelSeleccionado by remember { mutableIntStateOf(1) }
    var periodoSeleccionado by remember { mutableStateOf<String?>(null) }
    var mostrarLaboratorios by remember { mutableStateOf(false) }
    val data = state.coordinacion

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Coordinación", style = MaterialTheme.typography.headlineMedium)
            Text("Consulta y seguimiento de la planificación semestral")
        }
        if (state.cargando && data == null) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        data?.let { coordinacion ->
            val periodoId = periodoSeleccionado ?: coordinacion.periodo.id
            val periodo = coordinacion.periodos.find { it.id == periodoId } ?: coordinacion.periodo
            val planAgregado = coordinacion.planificacionesAgregadas.find { it.periodoId == periodoId }
            val planes = planAgregado?.bloques ?: coordinacion.planificaciones
            item { ResumenCoordinacion(coordinacion, periodo, planAgregado?.estado, planes.size) }
            item {
                Text("Ciclo académico")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    coordinacion.periodos.filter { it.cicloAcademico != null }.forEach { ciclo ->
                        FilterChip(
                            selected = periodoId == ciclo.id,
                            onClick = { periodoSeleccionado = ciclo.id },
                            label = { Text(if (ciclo.cicloAcademico == 1) "Mayo–Septiembre" else "Noviembre–Abril") },
                        )
                    }
                }
            }
            item {
                Text("Nivel académico")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    (1..10).forEach { value ->
                        FilterChip(
                            selected = nivelSeleccionado == value,
                            onClick = { nivelSeleccionado = value },
                            label = { Text("$value°") },
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    diasPlanificacion.forEach { dia ->
                        FilterChip(
                            selected = diaSeleccionado == dia,
                            onClick = { diaSeleccionado = dia },
                            label = { Text(etiquetaDia(dia)) },
                        )
                    }
                }
            }
            val planesDia = planes
                .filter {
                    it.diaSemana == diaSeleccionado && it.estado != "CANCELADA" &&
                        (it.nivel ?: 1) == nivelSeleccionado
                }
                .sortedBy { it.horaInicio }
            if (planesDia.isEmpty()) item { Text("No hay asignaciones para ${etiquetaDia(diaSeleccionado)}.") }
            items(planesDia, key = { it.id }) { plan ->
                AsignacionCard(plan, coordinacion)
            }
            item {
                OutlinedButton(onClick = { mostrarLaboratorios = !mostrarLaboratorios }) {
                    Text(if (mostrarLaboratorios) "Ocultar disponibilidad" else "Disponibilidad de laboratorios")
                }
            }
            if (mostrarLaboratorios) {
                items(coordinacion.laboratorios, key = { it.id }) { laboratorio ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(laboratorio.codigo, style = MaterialTheme.typography.titleMedium)
                            Text(laboratorio.nombre)
                            Text(estadoLaboratorio(laboratorio.estado))
                        }
                    }
                }
            }
            item {
                Text("Las notificaciones de revisión, propuestas y aprobación se reciben mediante las notificaciones de la aplicación.")
                Spacer(Modifier.height(4.dp))
                Text("La creación, edición y el envío completo se realizan principalmente desde la Web.")
            }
        }
    }
}

@Composable
private fun ResumenCoordinacion(data: CoordinacionData, periodo: PeriodoPlanificacionDto,
    estado: String?, totalBloques: Int) {
    val carreraId = data.planificacion?.carreraId ?: data.materias.firstOrNull()?.carreraId
    val carrera = data.carreras.find { it.id == carreraId }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Carrera: ${carrera?.nombre ?: "Mi carrera institucional"}")
            Text("Periodo: ${periodo.ppaNombre ?: periodo.codigo}")
            Text("Ciclo académico: ${if (periodo.cicloAcademico == 1) "Mayo–Septiembre" else "Noviembre–Abril"}")
            Text("Estado de planificación: ${estado ?: estadoGeneral(data.planificaciones)}")
            Text("$totalBloques asignaciones")
        }
    }
}

@Composable
private fun AsignacionCard(plan: PlanificacionDto, data: CoordinacionData) {
    val materia = data.materias.find { it.id == plan.materiaId }
    val docente = data.docentes.find { it.id == plan.docenteId }
    val laboratorio = data.laboratorios.find { it.id == plan.laboratorioId }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${plan.horaInicio} - ${plan.horaFin}", style = MaterialTheme.typography.titleMedium)
            Text(materia?.nombre ?: "Materia no disponible")
            Text("Docente: ${docente?.codigoDocente ?: "Asignado"}")
            Text(laboratorio?.codigo ?: "Laboratorio no disponible")
            Text("Estado: ${etiquetaEstado(plan.estado)}")
            plan.observacion?.takeIf { it.isNotBlank() }?.let {
                Text("Observación: $it", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

internal fun estadoGeneral(planes: List<PlanificacionDto>): String {
    val vigentes = planes.filter { it.estado != "CANCELADA" }
    return when {
        vigentes.any { it.estado == "PROPUESTA_CAMBIO" } -> "DEVUELTA CON OBSERVACIONES"
        vigentes.any { it.estado == "BORRADOR" } -> "BORRADOR"
        vigentes.isNotEmpty() && vigentes.all { it.estado == "CONFIRMADA" } -> "APROBADA"
        vigentes.any { it.estado == "ENVIADA" } -> "EN REVISIÓN"
        vigentes.any { it.estado == "RECHAZADA" } -> "RECHAZADA"
        else -> "SIN INICIAR"
    }
}

internal fun etiquetaEstado(estado: String): String = when (estado) {
    "ENVIADA" -> "En revisión"
    "PROPUESTA_CAMBIO" -> "Devuelta con observaciones"
    "CONFIRMADA" -> "Aprobada"
    "BORRADOR" -> "Borrador"
    "RECHAZADA" -> "Rechazada"
    "CANCELADA" -> "Cancelada"
    else -> estado.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

private fun etiquetaDia(dia: String): String =
    if (dia == "MIERCOLES") "Miércoles" else dia.lowercase().replaceFirstChar(Char::uppercase)

private fun estadoLaboratorio(estado: String): String = when (estado) {
    "DISPONIBLE" -> "Disponible"
    "OCUPADO" -> "Ocupado"
    "MANTENIMIENTO" -> "En mantenimiento"
    else -> estado.lowercase().replaceFirstChar(Char::uppercase)
}

@Composable
fun HistorialAsistenciaScreen(viewModel: InstitutionalViewModel, estudiante: Boolean = false) {
    if (estudiante) {
        RegistroLaboratorioEstudianteScreen(viewModel)
        return
    }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarHistorial() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mi asistencia") }
        if (state.cargando) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it) } }
        state.mensaje?.let { item { Text(it) } }
        if (!state.cargando && state.historial.isEmpty()) item { Text("Todavía no hay registros") }
        items(state.historial, key = { it.id }) { registro ->
            Column { Text(registro.registradaEn); Text(registro.estado) }
        }
    }
}

@Composable
fun RegistroLaboratorioEstudianteScreen(viewModel: InstitutionalViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarEstudiante() }
    RegistroLaboratorioEstudianteContent(
        state = state,
        onRegistrar = viewModel::registrarPresencia,
        onActualizar = viewModel::cargarEstudiante,
    )
}

@Composable
internal fun RegistroLaboratorioEstudianteContent(
    state: InstitutionalUiState,
    onRegistrar: (String) -> Unit,
    onActualizar: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Registro de laboratorio", style = MaterialTheme.typography.headlineMedium)
            Text("Tu identidad se obtiene de la sesión autenticada.")
        }
        if (state.cargando && state.sesionesAbiertas.isEmpty()) item { CircularProgressIndicator() }
        state.error?.let { item {
            Text("No fue posible consultar los registros. Intenta nuevamente.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onActualizar) { Text("Reintentar") }
        } }
        state.mensaje?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        if (!state.cargando && state.error == null && state.sesionesAbiertas.isEmpty()) item {
            Text("No hay registros de laboratorio habilitados en este momento.")
        }
        items(state.sesionesAbiertas, key = { it.id }) { sesion ->
            val registrado = state.historial.any { it.sesionId == sesion.id }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Registro habilitado", style = MaterialTheme.typography.titleMedium)
                    Text("Disponible hasta ${sesion.expiraEn}")
                    if (registrado) Text("Tu presencia ya fue registrada en esta sesión.")
                    else Button(onClick = { onRegistrar(sesion.id) }, enabled = !state.cargando) { Text("Registrar mi presencia") }
                }
            }
        }
        item { Text("Mi historial de presencia", style = MaterialTheme.typography.titleLarge) }
        if (!state.cargando && state.historial.isEmpty()) item { Text("Aún no tienes registros de uso.") }
        items(state.historial, key = { it.id }) { registro ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(registro.registradaEn)
                    Text(if (registro.estado == "PRESENTE") "Registrado" else registro.estado.lowercase().replaceFirstChar(Char::uppercase))
                }
            }
        }
        item { Text("También puedes usar Escanear QR cuando el responsable muestre un código de registro.") }
    }
}

@Composable
fun AdministracionGlobalScreen(viewModel: InstitutionalViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarAdministracion() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Supervisión global", style = MaterialTheme.typography.headlineMedium) }
        if (state.cargando && state.administracion == null) item { CircularProgressIndicator() }
        state.error?.let { item {
            Text("No fue posible cargar la información global.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = viewModel::cargarAdministracion) { Text("Reintentar") }
        } }
        state.administracion?.let { data ->
            item {
                val activos = data.perfiles.count { it.activo }
                Text("Usuarios", style = MaterialTheme.typography.titleLarge)
                Text("$activos activos de ${data.perfiles.size} perfiles")
            }
            item {
                Text("Laboratorios", style = MaterialTheme.typography.titleLarge)
                Text("${data.laboratorios.size} registrados · ${data.laboratorios.count { it.estado == "DISPONIBLE" }} disponibles")
            }
            item {
                Text("Planificación", style = MaterialTheme.typography.titleLarge)
                Text("${data.planificaciones.size} asignaciones globales")
            }
            item { Text("Usuarios recientes", style = MaterialTheme.typography.titleLarge) }
            items(data.perfiles.take(20), key = { it.id }) { perfil ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text("${perfil.nombres} ${perfil.apellidos}", style = MaterialTheme.typography.titleMedium)
                    Text(perfil.emailInstitucional)
                    Text(if (perfil.activo) "Activo" else "Inactivo")
                } }
            }
        }
    }
}

@Composable
fun HorarioDocenteScreen(viewModel: InstitutionalViewModel, perfilId: String) {
    val state by viewModel.uiState.collectAsState()
    var dia by remember { mutableStateOf("LUNES") }
    LaunchedEffect(perfilId) { viewModel.cargarDocencia(perfilId) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mi semana", style = MaterialTheme.typography.headlineMedium) }
        if (state.cargando && state.docencia == null) item { CircularProgressIndicator() }
        state.error?.let { item {
            Text("No fue posible cargar tu horario.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { viewModel.cargarDocencia(perfilId) }) { Text("Reintentar") }
        } }
        state.docencia?.let { data ->
            item { Text("Periodo ${data.periodo.nombre}") }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                diasPlanificacion.forEach { value -> FilterChip(selected = dia == value, onClick = { dia = value }, label = { Text(value.take(3)) }) }
            } }
            val materias = data.materias.associateBy { it.id }
            val laboratorios = data.laboratorios.associateBy { it.id }
            val clases = data.horarios.filter { it.diaSemana == dia }
            if (clases.isEmpty()) item { Text("No tienes clases programadas para este día.") }
            items(clases, key = { it.id }) { clase ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${clase.horaInicio}–${clase.horaFin}", style = MaterialTheme.typography.titleMedium)
                    Text(materias[clase.materiaId]?.nombre ?: "Materia asignada")
                    Text(clase.laboratorioId?.let { laboratorios[it]?.let { lab -> "${lab.codigo} — ${lab.nombre}" } } ?: "Aula por confirmar")
                    Text("Planificación base · Solo lectura")
                } }
            }
            item { Text("Los cambios de una fecha concreta se gestionan como solicitudes y no modifican este horario base.") }
        }
    }
}
