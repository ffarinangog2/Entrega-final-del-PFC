package ec.edu.uteq.scli.mobile.features.reservas.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.EstadoPeriodoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.MateriaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PageResponse
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PeriodoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CatalogosSerializationTest {
    private val gson = Gson()

    @Test
    fun `deserializa la pagina real de Spring sin tratar content como respuesta raiz`() {
        val json = """
            {
              "content":[{"id":"m1","codigo":"MAT-1","nombre":"Redes","activo":true}],
              "pageable":{"pageNumber":0,"pageSize":20},
              "totalPages":2,"totalElements":21,"last":false,"size":20,"number":0,
              "sort":{"empty":true,"sorted":false,"unsorted":true},
              "numberOfElements":1,"first":true,"empty":false
            }
        """.trimIndent()
        val type = object : TypeToken<PageResponse<MateriaDto>>() {}.type

        val page: PageResponse<MateriaDto> = gson.fromJson(json, type)

        assertEquals("MAT-1", page.content.single().codigo)
        assertEquals(2, page.totalPages)
        assertFalse(page.last)
    }

    @Test
    fun `deserializa estado de periodo segun el contrato Backend`() {
        val periodo = gson.fromJson(
            """{"id":"p1","codigo":"2026-A","nombre":"Periodo 2026 A","estado":"ACTIVO"}""",
            PeriodoDto::class.java,
        )

        assertEquals(EstadoPeriodoDto.ACTIVO, periodo.estado)
    }
}
