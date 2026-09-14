package ec.edu.uteq.scli.mobile.features.qr.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

object QrCodeGenerator {
    fun construirPayloadAsistencia(sesionId: String, token: String): String =
        "scli-asistencia:$sesionId:$token"

    fun generarBitmap(contenido: String, ancho: Int = 512, alto: Int = 512): Bitmap? {
        if (contenido.isBlank()) return null
        return runCatching {
            val bitMatrix = MultiFormatWriter().encode(contenido, BarcodeFormat.QR_CODE, ancho, alto)
            val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.RGB_565)
            for (x in 0 until ancho) {
                for (y in 0 until alto) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        }.getOrNull()
    }
}
