package ec.edu.uteq.scli.mobile.features.reservas.domain

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReservaRepositoryDefaultsTest {
    private val repository = object : ReservaRepository {
        override suspend fun listar(pagina: Int, tamanio: Int) =
            NetworkResult.Failure(501, "no_usado")
        override suspend fun obtener(id: String) = NetworkResult.Failure(501, "no_usado")
        override suspend fun crearSolicitud(solicitud: NuevaSolicitudReserva, idempotencyKey: String) =
            NetworkResult.Failure(501, "no_usado")
        override suspend fun actualizarSolicitud(id: String, solicitud: ActualizacionSolicitudReserva) =
            NetworkResult.Failure(501, "no_usado")
        override suspend fun cancelarSolicitud(id: String, comentario: String) =
            NetworkResult.Failure(501, "no_usado")
        override suspend fun cancelarReserva(id: String, motivo: String) =
            NetworkResult.Failure(501, "no_usado")
        override suspend fun consultarDisponibilidad(
            laboratorioId: String,
            fecha: String,
            horaInicio: String,
            horaFin: String,
        ) = NetworkResult.Failure(501, "no_usado")
    }

    @Test
    fun `endpoints no sobrescritos devuelven no_implementado por defecto`() = runTest {
        val esperado = NetworkResult.Failure(null, "no_implementado")
        assertEquals(esperado, repository.listarSolicitudes())
        assertEquals(esperado, repository.obtenerSolicitud("id"))
        assertEquals(esperado, repository.historial("id"))
        assertEquals(esperado, repository.ponerEnRevision("id"))
        assertEquals(esperado, repository.aprobar("id", "responsable", null, "key"))
        assertEquals(esperado, repository.rechazar("id", "comentario"))
        assertEquals(
            esperado,
            repository.proponer("id", PropuestaAlternativa("lab", "fecha", "08:00", "10:00", null)),
        )
        assertEquals(esperado, repository.responderPropuesta("id", true, null))
    }
}
