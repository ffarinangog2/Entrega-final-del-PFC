package ec.edu.uteq.scli.mobile.features.notifications.presentation

import ec.edu.uteq.scli.mobile.common.navigation.AppDestination
import ec.edu.uteq.scli.mobile.common.navigation.MobileNavigationAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationNavigationResolverTest {

    private fun access(
        coordinador: Boolean = false,
        reservas: Boolean = false,
        calendario: Boolean = false,
        incidentes: Boolean = false,
        planificacion: Boolean = false,
        estudiante: Boolean = false,
        administrador: Boolean = false,
        docente: Boolean = false,
        asistencia: Boolean = false,
    ) = MobileNavigationAccess(
        coordinador = coordinador,
        reservas = reservas,
        calendario = calendario,
        incidentes = incidentes,
        planificacion = planificacion,
        estudiante = estudiante,
        administrador = administrador,
        docente = docente,
        asistencia = asistencia,
    )

    @Test
    fun `A - SOLICITUD con id y rol con permiso reservas resuelve a SolicitudDetalle`() {
        val docenteAccess = access(docente = true, reservas = true)
        val destino = resolveNotificationDestination("SOLICITUD", "sol-123", docenteAccess)
        assertEquals("solicitudes/sol-123", destino)
    }

    @Test
    fun `B - SOLICITUD con referenciaId nulo o vacio resuelve a null`() {
        val docenteAccess = access(docente = true, reservas = true)
        assertNull(resolveNotificationDestination("SOLICITUD", null, docenteAccess))
        assertNull(resolveNotificationDestination("SOLICITUD", "", docenteAccess))
        assertNull(resolveNotificationDestination("SOLICITUD", "   ", docenteAccess))
    }

    @Test
    fun `C - RESERVA con id y rol permitido resuelve a ReservaDetalle`() {
        val docenteAccess = access(docente = true, reservas = true)
        val destino = resolveNotificationDestination("RESERVA", "res-456", docenteAccess)
        assertEquals("reservas/res-456", destino)
    }

    @Test
    fun `D - ASISTENCIA_ABIERTA para ESTUDIANTE resuelve a Asistencia`() {
        val estudianteAccess = access(estudiante = true, asistencia = true)
        val destino = resolveNotificationDestination("ASISTENCIA_ABIERTA", null, estudianteAccess)
        assertEquals(AppDestination.Asistencia.route, destino)
    }

    @Test
    fun `E - CAMBIO_HORARIO para ESTUDIANTE resuelve a HorarioEstudiante`() {
        val estudianteAccess = access(estudiante = true)
        val destino = resolveNotificationDestination("CAMBIO_HORARIO", null, estudianteAccess)
        assertEquals(AppDestination.HorarioEstudiante.route, destino)
    }

    @Test
    fun `F - CAMBIO_HORARIO para DOCENTE resuelve a HorarioDocente`() {
        val docenteAccess = access(docente = true)
        val destino = resolveNotificationDestination("CAMBIO_HORARIO", null, docenteAccess)
        assertEquals(AppDestination.HorarioDocente.route, destino)
    }

    @Test
    fun `G - HORARIO_DOCENTE para DOCENTE resuelve a HorarioDocente`() {
        val docenteAccess = access(docente = true)
        val destino = resolveNotificationDestination("HORARIO_DOCENTE", null, docenteAccess)
        assertEquals(AppDestination.HorarioDocente.route, destino)
    }

    @Test
    fun `H - PLANIFICACION_APROBADA para COORDINADOR resuelve a Planificacion`() {
        val coordAccess = access(coordinador = true, planificacion = true)
        val destino = resolveNotificationDestination("PLANIFICACION_APROBADA", null, coordAccess)
        assertEquals(AppDestination.Planificacion.route, destino)
    }

    @Test
    fun `I - SOLICITUD_CAMBIO_RESUELTA para COORDINADOR resuelve a Planificacion`() {
        val coordAccess = access(coordinador = true, planificacion = true)
        val destino = resolveNotificationDestination("SOLICITUD_CAMBIO_RESUELTA", null, coordAccess)
        assertEquals(AppDestination.Planificacion.route, destino)
    }

    @Test
    fun `J - SOLICITUD_RETIRO para ADMINISTRADOR_PISO resuelve a Planificacion`() {
        val pisoAccess = access(planificacion = true)
        val destino = resolveNotificationDestination("SOLICITUD_RETIRO", null, pisoAccess)
        assertEquals(AppDestination.Planificacion.route, destino)
    }

    @Test
    fun `K - INCIDENTE_ACTUALIZADO para rol con acceso resuelve a Incidentes`() {
        val roleAccess = access(incidentes = true)
        val destino = resolveNotificationDestination("INCIDENTE_ACTUALIZADO", "inc-99", roleAccess)
        assertEquals(AppDestination.Incidentes.route, destino)
    }

    @Test
    fun `L - tipo desconocido resuelve a null`() {
        val adminAccess = access(administrador = true, reservas = true, incidentes = true, planificacion = true)
        assertNull(resolveNotificationDestination("TIPO_INEXISTENTE_XYZ", "123", adminAccess))
        assertNull(resolveNotificationDestination("", "123", adminAccess))
    }

    @Test
    fun `M - tipo conocido pero rol sin permiso resuelve a null`() {
        // Estudiante intentando acceder a SOLICITUD o RESERVA sin permiso reservas
        val estudianteSinReservas = access(estudiante = true, reservas = false)
        assertNull(resolveNotificationDestination("SOLICITUD", "sol-1", estudianteSinReservas))
        assertNull(resolveNotificationDestination("RESERVA", "res-1", estudianteSinReservas))

        // Estudiante intentando acceder a PLANIFICACION
        assertNull(resolveNotificationDestination("PLANIFICACION_APROBADA", null, estudianteSinReservas))

        // Usuario sin permiso de incidentes
        val sinIncidentes = access(incidentes = false)
        assertNull(resolveNotificationDestination("INCIDENTE_CREADO", null, sinIncidentes))
    }
}
