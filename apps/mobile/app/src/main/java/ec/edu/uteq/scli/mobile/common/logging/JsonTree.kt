package ec.edu.uteq.scli.mobile.common.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import timber.log.Timber

/**
 * Tree de Timber que emite cada línea como un único objeto JSON, incluyendo
 * el trace_id de la sesión actual para poder correlacionar logs.
 */
class JsonTree : Timber.Tree() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val json = JSONObject().apply {
            put("ts", dateFormat.format(Date()))
            put("level", priorityLabel(priority))
            put("tag", tag ?: "App")
            put("trace_id", TraceId.sessionId)
            put("message", message)
            if (t != null) {
                put("error", Log.getStackTraceString(t))
            }
        }

        Log.println(priority, tag ?: "App", json.toString())
    }

    private fun priorityLabel(priority: Int): String = when (priority) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        Log.ASSERT -> "ASSERT"
        else -> "UNKNOWN"
    }
}
