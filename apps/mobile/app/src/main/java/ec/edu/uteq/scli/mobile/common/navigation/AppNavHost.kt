package ec.edu.uteq.scli.mobile.common.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ec.edu.uteq.scli.mobile.R
import ec.edu.uteq.scli.mobile.ScliMobileApplication
import ec.edu.uteq.scli.mobile.features.incidentes.presentation.IncidentesScreen
import ec.edu.uteq.scli.mobile.features.incidentes.presentation.IncidentesViewModel
import ec.edu.uteq.scli.mobile.features.institutional.presentation.HistorialAsistenciaScreen
import ec.edu.uteq.scli.mobile.features.institutional.presentation.InstitutionalViewModel
import ec.edu.uteq.scli.mobile.features.institutional.presentation.PlanificacionesScreen
import ec.edu.uteq.scli.mobile.features.auth.presentation.AuthViewModel
import ec.edu.uteq.scli.mobile.features.auth.presentation.LoginScreen
import ec.edu.uteq.scli.mobile.features.profile.presentation.ProfileScreen
import ec.edu.uteq.scli.mobile.features.profile.presentation.ProfileViewModel
import ec.edu.uteq.scli.mobile.features.qr.presentation.QrScanScreen
import ec.edu.uteq.scli.mobile.features.qr.presentation.QrViewModel
import ec.edu.uteq.scli.mobile.features.reservas.presentation.NuevaReservaScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.NuevaReservaViewModel
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservaDetalleScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservasScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservasViewModel
import ec.edu.uteq.scli.mobile.features.reservas.presentation.SolicitudDetalleScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.CalendarioScreen
import ec.edu.uteq.scli.mobile.features.auth.data.hasAnyPermission
import ec.edu.uteq.scli.mobile.features.auth.data.hasPermission
import ec.edu.uteq.scli.mobile.features.auth.data.hasRole
import ec.edu.uteq.scli.mobile.features.auth.data.AuthUserResponse

private sealed class AppDestination(val route: String) {
    data object Incidentes : AppDestination("incidentes")
    data object Reservas : AppDestination("reservas")
    data object Perfil : AppDestination("perfil")
    data object Calendario : AppDestination("calendario")
    data object EscanearQr : AppDestination("escanear-qr")
    data object Planificacion : AppDestination("planificacion")
    data object Asistencia : AppDestination("asistencia")
    data object NuevaReserva : AppDestination("reservas/nueva")
    data object SolicitudDetalle : AppDestination("solicitudes/{solicitudId}") { fun crearRuta(id: String) = "solicitudes/$id" }
    data object ReservaDetalle : AppDestination("reservas/{reservaId}") {
        fun crearRuta(id: String) = "reservas/$id"
    }
}

internal data class MobileNavigationAccess(
    val coordinador: Boolean,
    val reservas: Boolean,
    val calendario: Boolean,
    val incidentes: Boolean,
    val planificacion: Boolean,
)

internal fun navigationAccess(user: AuthUserResponse): MobileNavigationAccess {
    val coordinador = user.hasRole("COORDINADOR")
    return MobileNavigationAccess(
        coordinador = coordinador,
        reservas = !coordinador && user.hasAnyPermission("RESERVA_LEER", "SOLICITUD_LEER"),
        calendario = !coordinador && user.hasAnyPermission("RESERVA_LEER", "AGENDA_GESTIONAR"),
        incidentes = !coordinador && user.hasAnyPermission("INCIDENTE_LEER", "INCIDENTE_CREAR", "INCIDENTE_GESTIONAR"),
        planificacion = user.hasAnyPermission("PLANIFICACION_GESTIONAR", "SOLICITUD_APROBAR"),
    )
}

@Composable
fun AppNavHost(application: ScliMobileApplication) {
    val container = application.container
    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(container.authRepository) }
        },
    )
    val authState by authViewModel.uiState.collectAsState()

    if (authState.restaurando) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
        return
    }
    if (authState.sesion == null) {
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
    val puedeVerAsistencia = user.hasAnyPermission("ASISTENCIA_LEER", "ASISTENCIA_GESTIONAR", "ASISTENCIA_REGISTRAR")

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar {
                if (puedeVerIncidentes) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Incidentes),
                    onClick = { navController.navigateToTab(AppDestination.Incidentes.route) },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_incidentes)) },
                )
                if (puedeVerCalendario) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Calendario),
                    onClick = { navController.navigateToTab(AppDestination.Calendario.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                    label = { Text("Calendario") },
                )
                if (puedeVerPlanificacion) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Planificacion),
                    onClick = { navController.navigateToTab(AppDestination.Planificacion.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = "Planificación") },
                    label = { Text("Planificación") },
                )
                if (puedeVerAsistencia) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Asistencia),
                    onClick = { navController.navigateToTab(AppDestination.Asistencia.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = "Asistencia") },
                    label = { Text("Asistencia") },
                )
                if (puedeVerReservas) NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Reservas),
                    onClick = { navController.navigateToTab(AppDestination.Reservas.route) },
                    icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_reservas)) },
                )
                NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Perfil),
                    onClick = { navController.navigateToTab(AppDestination.Perfil.route) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_perfil)) },
                )
                if (!coordinador) {
                    NavigationBarItem(
                        selected = currentDestination.isRoute(AppDestination.EscanearQr),
                        onClick = { navController.navigateToTab(AppDestination.EscanearQr.route) },
                        icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                        label = { Text("Escanear QR") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = when {
                puedeVerReservas -> AppDestination.Reservas.route
                puedeVerPlanificacion -> AppDestination.Planificacion.route
                puedeVerAsistencia -> AppDestination.Asistencia.route
                puedeVerIncidentes -> AppDestination.Incidentes.route
                else -> AppDestination.Perfil.route
            },
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Incidentes.route) {
                if (!puedeVerIncidentes) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: IncidentesViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            IncidentesViewModel(container.incidenteRepository, container.notificationHelper)
                        }
                    },
                )
                IncidentesScreen(viewModel, puedeGestionar = user.hasPermission("INCIDENTE_GESTIONAR"))
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
                        initializer { QrViewModel(container.qrRepository, container.institutionalRepository) }
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
                )
            }
            composable(AppDestination.Asistencia.route) {
                if (!puedeVerAsistencia) { Text("No tienes permisos para realizar esta acción."); return@composable }
                val viewModel: InstitutionalViewModel = viewModel(
                    factory = viewModelFactory { initializer { InstitutionalViewModel(container.institutionalRepository) } },
                )
                HistorialAsistenciaScreen(viewModel)
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
