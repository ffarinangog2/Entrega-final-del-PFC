package ec.edu.uteq.scli.mobile.common.navigation

import ec.edu.uteq.scli.mobile.features.auth.data.AuthUserResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileNavigationAccessTest {
    @Test
    fun `coordinador recibe planificacion sin modulos operativos`() {
        val access = navigationAccess(user("COORDINADOR", listOf("PLANIFICACION_GESTIONAR", "RESERVA_LEER", "INCIDENTE_LEER")))

        assertTrue(access.coordinador)
        assertTrue(access.planificacion)
        assertFalse(access.reservas)
        assertFalse(access.calendario)
        assertFalse(access.incidentes)
        assertFalse(access.estudiante)
    }

    @Test
    fun `otro rol conserva navegacion segun permisos`() {
        val access = navigationAccess(user("DOCENTE", listOf("RESERVA_LEER", "INCIDENTE_LEER")))

        assertFalse(access.coordinador)
        assertTrue(access.reservas)
        assertTrue(access.calendario)
        assertTrue(access.incidentes)
        assertFalse(access.planificacion)
        assertTrue(access.docente)
        assertFalse(access.estudiante)
    }

    @Test
    fun `estudiante recibe horario estudiante y no modulos no autorizados`() {
        val access = navigationAccess(user("ESTUDIANTE", listOf("ACADEMICO_LEER")))

        assertTrue(access.estudiante)
        assertFalse(access.docente)
        assertFalse(access.administrador)
        assertFalse(access.coordinador)
        assertFalse(access.reservas)
        assertFalse(access.calendario)
        assertFalse(access.incidentes)
        assertFalse(access.planificacion)
    }

    @Test
    fun `administrador recibe inicio global y modulos segun permisos pero no horario estudiante`() {
        val access = navigationAccess(user("ADMINISTRADOR", listOf("RESERVA_LEER", "INCIDENTE_LEER", "PLANIFICACION_GESTIONAR")))

        assertTrue(access.administrador)
        assertTrue(access.reservas)
        assertTrue(access.incidentes)
        assertTrue(access.planificacion)
        assertFalse(access.estudiante)
    }

    @Test
    fun `A - notificaciones es considerada ruta secundaria con back arrow`() {
        assertTrue(esRutaSecundaria(AppDestination.Notificaciones.route))
    }

    @Test
    fun `B - nueva reserva es considerada ruta secundaria`() {
        assertTrue(esRutaSecundaria(AppDestination.NuevaReserva.route))
    }

    @Test
    fun `C - reserva detalle es considerada ruta secundaria`() {
        assertTrue(esRutaSecundaria(AppDestination.ReservaDetalle.route))
    }

    @Test
    fun `D - solicitud detalle es considerada ruta secundaria`() {
        assertTrue(esRutaSecundaria(AppDestination.SolicitudDetalle.route))
    }

    @Test
    fun `E - rutas principales no muestran back arrow`() {
        assertFalse(esRutaSecundaria(AppDestination.HorarioDocente.route))
        assertFalse(esRutaSecundaria(AppDestination.HorarioEstudiante.route))
        assertFalse(esRutaSecundaria(AppDestination.Reservas.route))
        assertFalse(esRutaSecundaria(AppDestination.Perfil.route))
        assertFalse(esRutaSecundaria(AppDestination.Incidentes.route))
        assertFalse(esRutaSecundaria(AppDestination.Calendario.route))
        assertFalse(esRutaSecundaria(AppDestination.Planificacion.route))
        assertFalse(esRutaSecundaria(AppDestination.Asistencia.route))
        assertFalse(esRutaSecundaria(AppDestination.Administracion.route))
        assertFalse(esRutaSecundaria(AppDestination.EscanearQr.route))
        assertFalse(esRutaSecundaria(null))
        assertFalse(esRutaSecundaria(""))
    }

    private fun user(role: String, permissions: List<String>) = AuthUserResponse(
        "usuario", "perfil", "user", "Nombre", "Apellido", "user@uteq.edu.ec",
        roles = listOf(role), permisos = permissions,
    )
}
