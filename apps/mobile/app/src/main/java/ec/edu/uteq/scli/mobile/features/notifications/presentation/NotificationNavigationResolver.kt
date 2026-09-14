package ec.edu.uteq.scli.mobile.features.notifications.presentation

import ec.edu.uteq.scli.mobile.common.navigation.AppDestination
import ec.edu.uteq.scli.mobile.common.navigation.MobileNavigationAccess

internal fun resolveNotificationDestination(
    tipo: String,
    referenciaId: String?,
    access: MobileNavigationAccess,
): String? {
    val cleanRefId = referenciaId?.trim()?.takeIf { it.isNotEmpty() }
    val cleanTipo = tipo.trim().uppercase()

    return when {
        cleanTipo == "SOLICITUD" -> {
            if (cleanRefId != null && access.reservas) {
                AppDestination.SolicitudDetalle.crearRuta(cleanRefId)
            } else {
                null
            }
        }
        cleanTipo == "RESERVA" -> {
            if (cleanRefId != null && access.reservas) {
                AppDestination.ReservaDetalle.crearRuta(cleanRefId)
            } else {
                null
            }
        }
        cleanTipo == "ASISTENCIA_ABIERTA" -> {
            if (access.asistencia) {
                AppDestination.Asistencia.route
            } else {
                null
            }
        }
        cleanTipo == "CAMBIO_HORARIO" -> {
            when {
                access.estudiante -> AppDestination.HorarioEstudiante.route
                access.docente -> AppDestination.HorarioDocente.route
                else -> null
            }
        }
        cleanTipo == "HORARIO_DOCENTE" -> {
            if (access.docente) {
                AppDestination.HorarioDocente.route
            } else {
                null
            }
        }
        cleanTipo.startsWith("PLANIFICACION") -> {
            if (access.planificacion) {
                AppDestination.Planificacion.route
            } else {
                null
            }
        }
        cleanTipo.startsWith("SOLICITUD_CAMBIO") -> {
            if (access.planificacion) {
                AppDestination.Planificacion.route
            } else {
                null
            }
        }
        cleanTipo.startsWith("SOLICITUD_RETIRO") -> {
            if (access.planificacion) {
                AppDestination.Planificacion.route
            } else {
                null
            }
        }
        cleanTipo.startsWith("INCIDENTE") -> {
            if (access.incidentes) {
                AppDestination.Incidentes.route
            } else {
                null
            }
        }
        else -> null
    }
}
