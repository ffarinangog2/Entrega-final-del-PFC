package ec.edu.uteq.scli.mobile.common.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ec.edu.uteq.scli.mobile.R
import ec.edu.uteq.scli.mobile.ScliMobileApplication
import ec.edu.uteq.scli.mobile.features.auth.data.AuthUserResponse
import ec.edu.uteq.scli.mobile.features.auth.data.hasAnyPermission
import ec.edu.uteq.scli.mobile.features.auth.data.hasPermission
import ec.edu.uteq.scli.mobile.features.auth.data.hasRole
import ec.edu.uteq.scli.mobile.features.auth.presentation.AuthViewModel
import ec.edu.uteq.scli.mobile.features.auth.presentation.LoginScreen
import ec.edu.uteq.scli.mobile.features.incidentes.presentation.IncidentesScreen
import ec.edu.uteq.scli.mobile.features.incidentes.presentation.IncidentesViewModel
import ec.edu.uteq.scli.mobile.features.institutional.presentation.AdministracionGlobalScreen
import ec.edu.uteq.scli.mobile.features.institutional.presentation.HistorialAsistenciaScreen
import ec.edu.uteq.scli.mobile.features.institutional.presentation.HorarioDocenteScreen
import ec.edu.uteq.scli.mobile.features.institutional.presentation.HorarioEstudianteScreen
import ec.edu.uteq.scli.mobile.features.institutional.presentation.InstitutionalViewModel
import ec.edu.uteq.scli.mobile.features.institutional.presentation.PlanificacionesScreen
import ec.edu.uteq.scli.mobile.features.notifications.presentation.NotificationsScreen
import ec.edu.uteq.scli.mobile.features.notifications.presentation.NotificationsViewModel
import ec.edu.uteq.scli.mobile.features.notifications.presentation.resolveNotificationDestination
import ec.edu.uteq.scli.mobile.features.profile.presentation.ProfileScreen
import ec.edu.uteq.scli.mobile.features.profile.presentation.ProfileViewModel
import ec.edu.uteq.scli.mobile.features.qr.presentation.QrScanScreen
import ec.edu.uteq.scli.mobile.features.qr.presentation.QrViewModel
import ec.edu.uteq.scli.mobile.features.reservas.presentation.CalendarioScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.NuevaReservaScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.NuevaReservaViewModel
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservaDetalleScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservasScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservasViewModel
import ec.edu.uteq.scli.mobile.features.reservas.presentation.SolicitudDetalleScreen

internal sealed class AppDestination(val route: String) {
    data object Incidentes : AppDestination("incidentes")
    data object Reservas : AppDestination("reservas")
    data object Perfil : AppDestination("perfil")
    data object Calendario : AppDestination("calendario")
    data object EscanearQr : AppDestination("escanear-qr")
    data object Planificacion : AppDestination("planificacion")
    data object Asistencia : AppDestination("asistencia")
    data object Administracion : AppDestination("administracion")
    data object HorarioDocente : AppDestination("mi-horario")
    data object HorarioEstudiante : AppDestination("horario-estudiante")
    data object Notificaciones : AppDestination("notificaciones")
    data object NuevaReserva : AppDestination("reservas/nueva")
    data object SolicitudDetalle : AppDestination("solicitudes/{solicitudId}") { fun crearRuta(id: String) = "solicitudes/$id" }
    data object ReservaDetalle : AppDestination("reservas/{reservaId}") {
        fun crearRuta(id: String) = "reservas/$id"
    }
}

internal val rutasSecundarias: Set<String> = setOf(
    AppDestination.Notificaciones.route,
    AppDestination.NuevaReserva.route,
    AppDestination.SolicitudDetalle.route,
    AppDestination.ReservaDetalle.route,
)

internal fun esRutaSecundaria(route: String?): Boolean =
    route != null && route in rutasSecundarias

internal data class MobileNavigationAccess(
    val coordinador: Boolean = false,
    val reservas: Boolean = false,
    val calendario: Boolean = false,
    val incidentes: Boolean = false,
    val planificacion: Boolean = false,
    val estudiante: Boolean = false,
    val administrador: Boolean = false,
    val docente: Boolean = false,
    val asistencia: Boolean = false,
)

internal fun navigationAccess(user: AuthUserResponse): MobileNavigationAccess {
    val coordinador = user.hasRole("COORDINADOR")
    val estudiante = user.hasRole("ESTUDIANTE")
    val administrador = user.hasRole("ADMINISTRADOR")
    val docente = user.hasRole("DOCENTE")
    val asistencia = estudiante ||
        user.hasAnyPermission("ASISTENCIA_LEER", "ASISTENCIA_GESTIONAR", "ASISTENCIA_REGISTRAR")
    return MobileNavigationAccess(
        coordinador = coordinador,
        reservas = !coordinador && user.hasAnyPermission("RESERVA_LEER", "SOLICITUD_LEER"),
        calendario = !coordinador && !estudiante && user.hasAnyPermission("RESERVA_LEER", "AGENDA_GESTIONAR"),
        incidentes = !coordinador && user.hasAnyPermission("INCIDENTE_LEER", "INCIDENTE_CREAR", "INCIDENTE_GESTIONAR"),
        planificacion = user.hasAnyPermission("PLANIFICACION_GESTIONAR", "SOLICITUD_APROBAR"),
        estudiante = estudiante,
        administrador = administrador,
        docente = docente,
        asistencia = asistencia,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(application: ScliMobileApplication) {
    val container = application.container
    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(container.authRepository) }
        },
    )
    val authState by authViewModel.uiState.collectAsState()

    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NotificationsViewModel(container.notificationsRepository) }
        },
    )
    val notifState by notificationsViewModel.uiState.collectAsState()

    LaunchedEffect(authState.sesion) {
        if (authState.sesion != null) {
            notificationsViewModel.cargarNoLeidas()
        }
    }

    LaunchedEffect(Unit) {
        container.notificationsRepository.pushTrigger.collect {
            if (authState.sesion != null) {
                notificationsViewModel.cargarNoLeidas()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (authState.sesion != null) {
                    notificationsViewModel.cargarNoLeidas()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (authState.restaurando) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
        return
    }

    if (authState.sesion == null) {
        container.pushNavigationManager.clear()
        LoginScreen(authViewModel)
        return
    }

    val navController = rememberNavController()
    val user = requireNotNull(authState.sesion).usuario
    val access = navigationAccess(user)
    val coordinador = access.coordinador
    val puedeVerReservas = access.reservas
    val puedeCrearSolicitud = user.hasPermission("SOLICITUD_CREAR")
    val puedeVerCalendario = access.calendario
    val puedeVerIncidentes = access.incidentes
    val puedeVerPlanificacion = access.planificacion
    val puedeVerAsistencia = access.asistencia

    val pendingPushPayload by container.pushNavigationManager.pendingPayload.collectAsState()

    LaunchedEffect(pendingPushPayload, access) {
        if (pendingPushPayload != null) {
            val payload = container.pushNavigationManager.consumePayload() ?: return@LaunchedEffect
            val destino = resolveNotificationDestination(
                tipo = payload.tipo,
                referenciaId = payload.referenciaId,
                access = access,
            )
            if (destino != null) {
                navController.navigate(destino) {
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SCLI") },
                navigationIcon = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val puedeVolverAtras = esRutaSecundaria(currentRoute) ||
                        ((currentRoute == AppDestination.EscanearQr.route || currentRoute == AppDestination.Asistencia.route) &&
                            !access.estudiante && navController.previousBackStackEntry != null)
                    if (puedeVolverAtras) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                            )
                        }
                    }
                },
                actions = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    if (currentRoute != AppDestination.Notificaciones.route) {
                        IconButton(
                            onClick = {
                                navController.navigate(AppDestination.Notificaciones.route) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            BadgedBox(
                                badge = {
                                    if (notifState.cantidadNoLeidas > 0L) {
                                        Badge {
                                            Text(
                                                text = if (notifState.cantidadNoLeidas > 99L) "99+" else notifState.cantidadNoLeidas.toString(),
                                            )
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificaciones",
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar {
                if (access.administrador) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Administracion),
                    onClick = { navController.navigateToTab(AppDestination.Administracion.route) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Inicio") },
                )
                if (access.docente) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.HorarioDocente),
                    onClick = { navController.navigateToTab(AppDestination.HorarioDocente.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                    label = { Text("Horario") },
                )
                if (access.estudiante) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.HorarioEstudiante),
                    onClick = { navController.navigateToTab(AppDestination.HorarioEstudiante.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                    label = { Text("Mi Horario") },
                )
                if (access.estudiante) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Asistencia),
                    onClick = { navController.navigateToTab(AppDestination.Asistencia.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = "Asistencia") },
                    label = { Text("Asistencia") },
                )
                if (access.estudiante) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.EscanearQr),
                    onClick = { navController.navigateToTab(AppDestination.EscanearQr.route) },
                    icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                    label = { Text("Escanear QR") },
                )
                if (puedeVerPlanificacion) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Planificacion),
                    onClick = { navController.navigateToTab(AppDestination.Planificacion.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = "Planificación") },
                    label = { Text("Planificación") },
                )
                if (puedeVerReservas) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Reservas),
                    onClick = { navController.navigateToTab(AppDestination.Reservas.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_reservas)) },
                )
                if (puedeVerIncidentes) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Incidentes),
                    onClick = { navController.navigateToTab(AppDestination.Incidentes.route) },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_incidentes)) },
                )
                NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Perfil),
                    onClick = { navController.navigateToTab(AppDestination.Perfil.route) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_perfil)) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = when {
                access.administrador -> AppDestination.Administracion.route
                access.docente -> AppDestination.HorarioDocente.route
                access.estudiante -> AppDestination.HorarioEstudiante.route
                coordinador || user.hasRole("ADMINISTRADOR_PISO") -> AppDestination.Planificacion.route
                puedeVerReservas -> AppDestination.Reservas.route
                puedeVerPlanificacion -> AppDestination.Planificacion.route
                puedeVerAsistencia -> AppDestination.Asistencia.route
                puedeVerIncidentes -> AppDestination.Incidentes.route
                else -> AppDestination.Perfil.route
            },
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Notificaciones.route) {
                NotificationsScreen(
                    viewModel = notificationsViewModel,
                    access = access,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppDestination.Administracion.route) {
                if (!access.administrador) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: InstitutionalViewModel = viewModel(
                    factory = viewModelFactory { initializer { InstitutionalViewModel(container.institutionalRepository) } },
                )
                AdministracionGlobalScreen(
                    viewModel = viewModel,
                    onEscanearLaboratorio = {
                        navController.navigate(AppDestination.EscanearQr.route) { launchSingleTop = true }
                    },
                    onVerAsistencia = {
                        navController.navigate(AppDestination.Asistencia.route) { launchSingleTop = true }
                    },
                )
            }
            composable(AppDestination.HorarioDocente.route) {
                if (!access.docente) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: InstitutionalViewModel = viewModel(
                    factory = viewModelFactory { initializer { InstitutionalViewModel(container.institutionalRepository) } },
                )
                HorarioDocenteScreen(viewModel, user.perfilId)
            }
            composable(AppDestination.HorarioEstudiante.route) {
                if (!access.estudiante) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: InstitutionalViewModel = viewModel(
                    factory = viewModelFactory { initializer { InstitutionalViewModel(container.institutionalRepository) } },
                )
                HorarioEstudianteScreen(viewModel)
            }
            composable(AppDestination.Incidentes.route) {
                if (!puedeVerIncidentes) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: IncidentesViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            IncidentesViewModel(container.incidenteRepository, container.notificationHelper)
                        }
                    },
                )
                IncidentesScreen(
                    viewModel,
                    puedeGestionar = user.hasPermission("INCIDENTE_GESTIONAR"),
                    catalogos = container.catalogosRepository,
                )
            }
            composable(AppDestination.Perfil.route) {
                val viewModel: ProfileViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            ProfileViewModel(container.settingsRepository, container.profileRepository)
                        }
                    },
                )
                ProfileScreen(viewModel, onLogout = authViewModel::logout)
            }
            composable(AppDestination.Reservas.route) {
                val viewModel: ReservasViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ReservasViewModel(container.reservaRepository) }
                    },
                )
                ReservasScreen(
                    viewModel = viewModel,
                    onReservaClick = { navController.navigate(AppDestination.ReservaDetalle.crearRuta(it)) },
                    onNuevaReserva = { navController.navigate(AppDestination.NuevaReserva.route) },
                    onSolicitudClick = { navController.navigate(AppDestination.SolicitudDetalle.crearRuta(it)) },
                    puedeCrear = puedeCrearSolicitud,
                )
            }
            composable(AppDestination.EscanearQr.route) {
                val viewModel: QrViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            QrViewModel(
                                repository = container.qrRepository,
                                institutionalRepository = container.institutionalRepository,
                                esEstudiante = access.estudiante,
                            )
                        }
                    },
                )
                QrScanScreen(viewModel)
            }
            composable(AppDestination.Planificacion.route) {
                if (!puedeVerPlanificacion) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: InstitutionalViewModel = viewModel(
                    factory = viewModelFactory { initializer { InstitutionalViewModel(container.institutionalRepository) } },
                )
                PlanificacionesScreen(
                    viewModel,
                    puedeRevisar = user.hasPermission("SOLICITUD_APROBAR"),
                    coordinador = coordinador,
                    administradorPiso = user.hasRole("ADMINISTRADOR_PISO"),
                    onEscanearLaboratorio = {
                        navController.navigate(AppDestination.EscanearQr.route) { launchSingleTop = true }
                    },
                    onVerAsistencia = {
                        navController.navigate(AppDestination.Asistencia.route) { launchSingleTop = true }
                    },
                )
            }
            composable(AppDestination.Asistencia.route) {
                if (!puedeVerAsistencia) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: InstitutionalViewModel = viewModel(
                    factory = viewModelFactory { initializer { InstitutionalViewModel(container.institutionalRepository) } },
                )
                HistorialAsistenciaScreen(viewModel, estudiante = access.estudiante)
            }
            composable(AppDestination.NuevaReserva.route) {
                if (!puedeCrearSolicitud) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: NuevaReservaViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { NuevaReservaViewModel(container.reservaRepository, container.catalogosRepository, user.perfilId) }
                    },
                )
                NuevaReservaScreen(viewModel)
            }
            composable(AppDestination.Calendario.route) {
                if (!puedeVerCalendario) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: ReservasViewModel = viewModel(factory = viewModelFactory { initializer { ReservasViewModel(container.reservaRepository) } })
                CalendarioScreen(viewModel, container.catalogosRepository)
            }
            composable(AppDestination.SolicitudDetalle.route, arguments = listOf(navArgument("solicitudId") { type = NavType.StringType })) { entry ->
                if (!puedeVerReservas) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val id = requireNotNull(entry.arguments?.getString("solicitudId"))
                val viewModel: ReservasViewModel = viewModel(factory = viewModelFactory { initializer { ReservasViewModel(container.reservaRepository, false) } })
                SolicitudDetalleScreen(id, viewModel, user, container.catalogosRepository)
            }
            composable(
                route = AppDestination.ReservaDetalle.route,
                arguments = listOf(navArgument("reservaId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val reservaId = requireNotNull(backStackEntry.arguments?.getString("reservaId"))
                val viewModel: ReservasViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ReservasViewModel(container.reservaRepository, cargarInicialmente = false) }
                    },
                )
                val institutionalViewModel: InstitutionalViewModel = viewModel(
                    factory = viewModelFactory { initializer { InstitutionalViewModel(container.institutionalRepository) } },
                )
                ReservaDetalleScreen(
                    reservaId,
                    viewModel,
                    institutionalViewModel,
                    puedeCancelar = user.hasPermission("RESERVA_CANCELAR"),
                    puedeGestionarAsistencia = user.hasPermission("ASISTENCIA_GESTIONAR"),
                )
            }
        }
    }
}

private fun androidx.navigation.NavDestination?.isRoute(destination: AppDestination): Boolean =
    this?.hierarchy?.any { it.route == destination.route } == true

private fun androidx.navigation.NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
