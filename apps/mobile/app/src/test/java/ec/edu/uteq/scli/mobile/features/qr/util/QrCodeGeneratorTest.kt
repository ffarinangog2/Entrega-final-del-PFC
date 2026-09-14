package ec.edu.uteq.scli.mobile.features.qr.util

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class QrCodeGeneratorTest {

    @Test
    fun `construirPayloadAsistencia genera formato compatible con backend y scanner`() {
        val payload = QrCodeGenerator.construirPayloadAsistencia("sesion-123", "token-abc")
        assertEquals("scli-asistencia:sesion-123:token-abc", payload)
    }

    @Test
    fun `generarBitmap genera bitmap valido para payload no vacio`() {
        val bitmap = QrCodeGenerator.generarBitmap("scli-asistencia:sesion-123:token-abc", 128, 128)
        assertNotNull(bitmap)
        assertEquals(128, bitmap?.width)
        assertEquals(128, bitmap?.height)
    }

    @Test
    fun `generarBitmap devuelve null para payload en blanco`() {
        val bitmap = QrCodeGenerator.generarBitmap("")
        assertNull(bitmap)
    }
}
