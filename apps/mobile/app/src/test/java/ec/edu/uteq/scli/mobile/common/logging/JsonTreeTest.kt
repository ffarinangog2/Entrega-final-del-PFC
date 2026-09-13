package ec.edu.uteq.scli.mobile.common.logging

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// JsonTree.log es protected (heredado de Timber.Tree); se invoca por reflexión para no alterar la visibilidad de producción.
private fun JsonTree.logPublic(priority: Int, tag: String?, message: String, t: Throwable?) {
    val method = JsonTree::class.java.getDeclaredMethod(
        "log", Int::class.java, String::class.java, String::class.java, Throwable::class.java,
    )
    method.isAccessible = true
    method.invoke(this, priority, tag, message, t)
}

class JsonTreeTest {
    private val tree = JsonTree()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.getStackTraceString(any()) } returns "stack-trace"
        every { Log.println(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `log emite json con tag y mensaje`() {
        val messageSlot = slot<String>()

        tree.logPublic(Log.INFO, "MiTag", "hola mundo", null)

        verify { Log.println(Log.INFO, "MiTag", capture(messageSlot)) }
        val json = JSONObject(messageSlot.captured)
        assertEquals("INFO", json.getString("level"))
        assertEquals("MiTag", json.getString("tag"))
        assertEquals("hola mundo", json.getString("message"))
        assertTrue(json.has("trace_id"))
        assertTrue(json.has("ts"))
    }

    @Test
    fun `log usa App como tag por defecto y agrega el error`() {
        val messageSlot = slot<String>()
        val error = IllegalStateException("fallo")

        tree.logPublic(Log.ERROR, null, "algo fallo", error)

        verify { Log.println(Log.ERROR, "App", capture(messageSlot)) }
        val json = JSONObject(messageSlot.captured)
        assertEquals("App", json.getString("tag"))
        assertEquals("ERROR", json.getString("level"))
        assertEquals("stack-trace", json.getString("error"))
    }

    @Test
    fun `mapea todas las prioridades conocidas`() {
        val prioridades = mapOf(
            Log.VERBOSE to "VERBOSE",
            Log.DEBUG to "DEBUG",
            Log.INFO to "INFO",
            Log.WARN to "WARN",
            Log.ERROR to "ERROR",
            Log.ASSERT to "ASSERT",
            99 to "UNKNOWN",
        )
        val messageSlot = slot<String>()

        prioridades.forEach { (priority, label) ->
            tree.logPublic(priority, "Tag", "msg", null)
            verify { Log.println(priority, "Tag", capture(messageSlot)) }
            assertEquals(label, JSONObject(messageSlot.captured).getString("level"))
        }
    }
}
