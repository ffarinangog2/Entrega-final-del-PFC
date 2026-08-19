package ec.edu.scli.academico.dto.internal;

import ec.edu.scli.academico.enums.EstadoLaboratorio;

import java.util.UUID;

/**
 * DTO interno consumido por Reservas Service.
 * Este microservicio SOLO entrega información estructural y de estado
 * general del laboratorio. La disponibilidad por fecha y hora específica
 * es responsabilidad exclusiva de Reservas y Solicitudes Service.
 */
public record LaboratorioDisponibilidadBaseResponse(

        UUID laboratorioId,

        boolean existe,

        boolean activo,

        EstadoLaboratorio estado,

        Integer capacidad

) {
}
