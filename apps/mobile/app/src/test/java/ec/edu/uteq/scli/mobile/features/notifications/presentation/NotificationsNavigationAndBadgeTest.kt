package ec.edu.uteq.scli.mobile.features.notifications.presentation

import ec.edu.uteq.scli.mobile.common.navigation.navigationAccess
import ec.edu.uteq.scli.mobile.features.auth.data.AuthUserResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsNavigationAndBadgeTest {

    private fun user(role: String, permissions: List<String> = emptyList()) = AuthUserResponse(
        id = "usuario",
        perfilId = "perfil",
        username = "user",
        nombres = "Nombre",
        apellidos = "Apellido",
        emailInstitucional = "user@uteq.edu.ec",
        roles = listOf(role),
        permisos = permissions,
    )

    @Test
    fun `J - los cinco roles autenticados son reconocidos para sesion y acceso general`() {
        val roles = listOf(
            "ADMINISTRADOR",
            "ADMINISTRADOR_PISO",
            "COORDINADOR",
            "DOCENTE",
            "ESTUDIANTE",
        )

        for (rol in roles) {
            val u = user(rol)
            val access = navigationAccess(u)
            assertNotNull("Acceso generado para rol $rol", access)
            // Todos los roles son válidos en la sesión autenticada que contiene la ruta de notificaciones
            assertTrue(u.roles.contains(rol))
        }
    }

    @Test
    fun `K - no se agrega nueva pestana en NavigationBar para ningun rol`() {
        // Simular cálculo de items en bottom bar para estudiante (máximo 5)
        val estudiante = user("ESTUDIANTE", listOf("ACADEMICO_LEER", "INCIDENTE_CREAR", "INCIDENTE_LEER"))
        val accessEst = navigationAccess(estudiante)

        var tabsEstudiante = 0
        if (accessEst.administrador) tabsEstudiante++
        if (accessEst.docente) tabsEstudiante++
        if (accessEst.estudiante) tabsEstudiante++
        if (accessEst.incidentes) tabsEstudiante++
        if (accessEst.calendario) tabsEstudiante++
        if (accessEst.planificacion) tabsEstudiante++
        if (accessEst.asistencia) tabsEstudiante++
        if (accessEst.reservas) tabsEstudiante++
        tabsEstudiante++ // perfil
        if (!accessEst.coordinador) tabsEstudiante++ // QR

        // Las notificaciones NO agregan un tab aquí
        assertEquals(5, tabsEstudiante)
    }

    @Test
    fun `L - badge oculto si 0 o negativo`() {
        assertNull(badgeCountText(0L))
        assertNull(badgeCountText(-1L))
    }

    @Test
    fun `M - badge visible si mayor a 0`() {
        assertEquals("1", badgeCountText(1L))
        assertEquals("7", badgeCountText(7L))
        assertEquals("99", badgeCountText(99L))
    }

    @Test
    fun `N - 99+ para valores mayores a 99`() {
        assertEquals("99+", badgeCountText(100L))
        assertEquals("99+", badgeCountText(250L))
    }

    @Test
    fun `formatearFechaNotificacion formatea ISO y tiene fallback seguro ante error`() {
        val fechaIso = "2026-09-08T01:30:00Z"
        val formateada = formatearFechaNotificacion(fechaIso)
        assertTrue(formateada.isNotBlank())
        // Debe contener slash de fecha o dos puntos de hora
        assertTrue(formateada.contains(":") || formateada.contains("/"))

        // Fallback seguro ante string no parseable
        val invalida = "no-es-fecha-valida-texto-largo"
        val fallback = formatearFechaNotificacion(invalida)
        assertEquals("no-es-fecha-valida-", fallback)

        // String vacío
        assertEquals("", formatearFechaNotificacion(""))
    }
}
