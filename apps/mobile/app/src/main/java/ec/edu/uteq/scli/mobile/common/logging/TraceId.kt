package ec.edu.uteq.scli.mobile.common.logging

import java.util.UUID

/**
 * Identificador de sesión de la app, usado como trace_id en cada línea de log
 * para poder correlacionar todo lo que pasó durante una misma ejecución.
 */
object TraceId {
    val sessionId: String = UUID.randomUUID().toString()
}
