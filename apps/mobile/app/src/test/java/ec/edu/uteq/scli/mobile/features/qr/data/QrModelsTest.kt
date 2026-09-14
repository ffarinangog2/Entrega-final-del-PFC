package ec.edu.uteq.scli.mobile.features.qr.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class QrModelsTest {
    @Test
    fun `deserializa piso con la forma real de detalle completo`() {
        val dto = Gson().fromJson(
            """{"laboratorio":{"id":"lab-1","codigo":"LAB-1","nombre":"Redes"},"piso":{"id":"piso-1","numero":2,"descripcion":"Planta alta"},"bloque":{"id":"b1","codigo":"B1","nombre":"Bloque 1"},"campus":{"id":"c1","codigo":"C1","nombre":"Central"},"equipos":[]}""",
            LaboratorioDetalleDto::class.java,
        )

        val detalle = dto.toDomain()
        assertEquals("2", detalle.piso?.codigo)
        assertEquals("Planta alta", detalle.piso?.nombre)
    }

    @Test
    fun `mapea detalle completo con ubicaciones y equipos validos`() {
        val dto = LaboratorioDetalleDto(
            laboratorio = LaboratorioDto(
                id = "lab-1", codigo = "LAB-1", nombre = "Redes", capacidad = 30,
                descripcion = "Laboratorio de redes", estado = "ACTIVO", activo = true,
            ),
            piso = PisoDto(id = "piso-1", numero = 1, descripcion = "Piso 1"),
            bloque = UbicacionDto(id = "bloque-1", codigo = "B1", nombre = "Bloque A"),
            campus = UbicacionDto(id = "campus-1", codigo = "C1", nombre = "Campus Central"),
            equipos = listOf(
                EquipoDto(
                    id = "equipo-1", codigoInventario = "INV-1", numeroSerie = "SN-1",
                    marca = "Cisco", modelo = "X1", estado = "OPERATIVO", activo = true,
                ),
                EquipoDto(
                    id = null, codigoInventario = "INV-2", numeroSerie = "SN-2",
                    marca = "HP", modelo = "X2", estado = "OPERATIVO", activo = true,
                ),
            ),
        )

        val detalle = dto.toDomain()

        assertEquals("lab-1", detalle.laboratorio.id)
        assertEquals("LAB-1", detalle.laboratorio.codigo)
        assertEquals("Redes", detalle.laboratorio.nombre)
        assertEquals(30, detalle.laboratorio.capacidad)
        assertEquals("1", detalle.piso?.codigo)
        assertEquals("Piso 1", detalle.piso?.nombre)
        assertEquals("B1", detalle.bloque?.codigo)
        assertEquals("C1", detalle.campus?.codigo)
        assertEquals(1, detalle.equipos.size)
        assertEquals("equipo-1", detalle.equipos.single().id)
    }

    @Test
    fun `mapea codigo y nombre vacios cuando el dto trae nulos`() {
        val dto = LaboratorioDetalleDto(
            laboratorio = LaboratorioDto(
                id = "lab-2", codigo = null, nombre = null, capacidad = null,
                descripcion = null, estado = null, activo = null,
            ),
            piso = null,
            bloque = null,
            campus = null,
            equipos = emptyList(),
        )

        val detalle = dto.toDomain()

        assertEquals("lab-2", detalle.laboratorio.id)
        assertEquals("", detalle.laboratorio.codigo)
        assertEquals("", detalle.laboratorio.nombre)
        assertNull(detalle.laboratorio.capacidad)
        assertNull(detalle.piso)
        assertNull(detalle.bloque)
        assertNull(detalle.campus)
        assertEquals(emptyList<Equipo>(), detalle.equipos)
    }

    @Test
    fun `lanza excepcion cuando el laboratorio del dto es nulo`() {
        val dto = LaboratorioDetalleDto(
            laboratorio = null,
            piso = null,
            bloque = null,
            campus = null,
            equipos = emptyList(),
        )

        assertThrows(IllegalArgumentException::class.java) { dto.toDomain() }
    }

    @Test
    fun `lanza excepcion cuando el laboratorio no trae id`() {
        val dto = LaboratorioDetalleDto(
            laboratorio = LaboratorioDto(
                id = null, codigo = "LAB-3", nombre = "Quimica", capacidad = null,
                descripcion = null, estado = null, activo = null,
            ),
            piso = null,
            bloque = null,
            campus = null,
            equipos = emptyList(),
        )

        assertThrows(IllegalArgumentException::class.java) { dto.toDomain() }
    }
}
