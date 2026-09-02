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
    }

    @Test
    fun `otro rol conserva navegacion segun permisos`() {
        val access = navigationAccess(user("DOCENTE", listOf("RESERVA_LEER", "INCIDENTE_LEER")))

        assertFalse(access.coordinador)
        assertTrue(access.reservas)
        assertTrue(access.calendario)
        assertTrue(access.incidentes)
        assertFalse(access.planificacion)
    }

    private fun user(role: String, permissions: List<String>) = AuthUserResponse(
        "usuario", "perfil", "user", "Nombre", "Apellido", "user@uteq.edu.ec",
        roles = listOf(role), permisos = permissions,
    )
}
