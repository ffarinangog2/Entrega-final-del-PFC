package ec.edu.uteq.scli.mobile.common.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.padding
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
import ec.edu.uteq.scli.mobile.features.auth.presentation.AuthViewModel
import ec.edu.uteq.scli.mobile.features.auth.presentation.LoginScreen
import ec.edu.uteq.scli.mobile.features.profile.presentation.ProfileScreen
import ec.edu.uteq.scli.mobile.features.profile.presentation.ProfileViewModel
import ec.edu.uteq.scli.mobile.features.reservas.presentation.NuevaReservaScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.NuevaReservaViewModel
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservaDetalleScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservasScreen
import ec.edu.uteq.scli.mobile.features.reservas.presentation.ReservasViewModel

private sealed class AppDestination(val route: String) {
    data object Incidentes : AppDestination("incidentes")
    data object Reservas : AppDestination("reservas")
    data object Perfil : AppDestination("perfil")
    data object NuevaReserva : AppDestination("reservas/nueva")
    data object ReservaDetalle : AppDestination("reservas/{reservaId}") {
        fun crearRuta(id: String) = "reservas/$id"
    }
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

    if (authState.restaurando) return
    if (authState.sesion == null) {
        LoginScreen(authViewModel)
        return
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination.isRoute(AppDestination.Incidentes),
                    onClick = { navController.navigateToTab(AppDestination.Incidentes.route) },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_incidentes)) },
                )
                NavigationBarItem(
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
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Incidentes.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Incidentes.route) {
                val viewModel: IncidentesViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            IncidentesViewModel(container.incidenteRepository, container.notificationHelper)
                        }
                    },
                )
                IncidentesScreen(viewModel)
            }
            composable(AppDestination.Perfil.route) {
                val viewModel: ProfileViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            ProfileViewModel(container.settingsRepository)
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
                )
            }
            composable(AppDestination.NuevaReserva.route) {
                val viewModel: NuevaReservaViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { NuevaReservaViewModel(container.reservaRepository) }
                    },
                )
                NuevaReservaScreen(viewModel)
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
                ReservaDetalleScreen(reservaId, viewModel)
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
