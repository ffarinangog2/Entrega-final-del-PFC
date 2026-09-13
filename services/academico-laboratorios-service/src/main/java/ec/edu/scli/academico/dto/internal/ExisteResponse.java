package ec.edu.scli.academico.dto.internal;

import java.util.UUID;

/**
 * DTO interno genérico para los endpoints /internal/{recurso}/{id}/exists
 * consumidos por otros microservicios (Reservas y Solicitudes Service).
 * Se reutiliza para laboratorios, materias y periodos lectivos.
 */
public record ExisteResponse(

        UUID id,

        boolean existe

) {
}
