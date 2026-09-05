package ec.edu.uteq.scli.mobile.features.auth.data

import org.junit.Assert.*
import org.junit.Test

class AuthorizationTest {
    @Test fun `permisos controlan navegacion institucional`() {
        val docente = user(listOf("DOCENTE"), listOf("SOLICITUD_CREAR", "RESERVA_LEER"))
        val estudiante = user(listOf("ESTUDIANTE"), listOf("RESERVA_LEER"))
        val adminPiso = user(listOf("ADMINISTRADOR_PISO"), listOf("SOLICITUD_APROBAR", "SOLICITUD_RECHAZAR"))
        val coordinador = user(listOf("COORDINADOR"), listOf("PLANIFICACION_GESTIONAR"))
        assertTrue(docente.hasPermission("SOLICITUD_CREAR"))
        assertFalse(estudiante.hasPermission("SOLICITUD_CREAR"))
        assertTrue(adminPiso.hasAnyPermission("SOLICITUD_APROBAR", "SOLICITUD_RECHAZAR"))
        assertFalse(coordinador.hasPermission("SOLICITUD_APROBAR"))
    }

    private fun user(roles: List<String>, permisos: List<String>) =
        AuthUserResponse("u", "p", "user", "", "", "", roles, permisos)
}
