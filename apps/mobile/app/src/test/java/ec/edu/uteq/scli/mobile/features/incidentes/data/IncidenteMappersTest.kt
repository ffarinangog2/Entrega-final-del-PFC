package ec.edu.uteq.scli.mobile.features.incidentes.data

import ec.edu.uteq.scli.mobile.features.incidentes.domain.Incidente
import ec.edu.uteq.scli.mobile.features.incidentes.domain.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidenteMappersTest {
    @Test
    fun `mapea entidad a dominio traduciendo la prioridad`() {
        val entity = IncidenteEntity(
            id = 5,
            laboratorioEquipo = "Lab 1 - PC 3",
            descripcion = "No enciende",
            prioridad = "ALTA",
            fechaMillis = 1_000L,
            creadoEnMillis = 2_000L,
        )

        val incidente = entity.toDomain()

        assertEquals(entity.id, incidente.id)
        assertEquals(entity.laboratorioEquipo, incidente.laboratorioEquipo)
        assertEquals(entity.descripcion, incidente.descripcion)
        assertEquals(Prioridad.ALTA, incidente.prioridad)
        assertEquals(entity.fechaMillis, incidente.fechaMillis)
        assertEquals(entity.creadoEnMillis, incidente.creadoEnMillis)
    }

    @Test
    fun `mapea dominio a entidad serializando la prioridad como texto`() {
        val incidente = Incidente(
            id = 7,
            laboratorioEquipo = "Lab 2 - Proyector",
            descripcion = "Sin señal",
            prioridad = Prioridad.BAJA,
            fechaMillis = 3_000L,
            creadoEnMillis = 4_000L,
        )

        val entity = incidente.toEntity()

        assertEquals(incidente.id, entity.id)
        assertEquals(incidente.laboratorioEquipo, entity.laboratorioEquipo)
        assertEquals(incidente.descripcion, entity.descripcion)
        assertEquals("BAJA", entity.prioridad)
        assertEquals(incidente.fechaMillis, entity.fechaMillis)
        assertEquals(incidente.creadoEnMillis, entity.creadoEnMillis)
    }

    @Test
    fun `round trip entidad a dominio y de vuelta conserva los datos`() {
        val original = IncidenteEntity(
            id = 9,
            laboratorioEquipo = "Lab 3 - Router",
            descripcion = "Sin conexion",
            prioridad = "MEDIA",
            fechaMillis = 5_000L,
            creadoEnMillis = 6_000L,
        )

        val roundTrip = original.toDomain().toEntity()

        assertEquals(original, roundTrip)
    }
}
