package ec.edu.scli.academico.application.facade;

import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioDetalleCompletoResponse;

import java.util.UUID;

/**
 * Facade (patrón GoF) que oculta la coordinación entre los servicios de
 * Laboratorio, Piso, Bloque, Campus y Equipo, exponiendo una única
 * operación simple: obtener la ficha completa de un laboratorio.
 */
public interface LaboratorioDetalleFacade {

    LaboratorioDetalleCompletoResponse obtenerDetalleCompleto(UUID laboratorioId);
}