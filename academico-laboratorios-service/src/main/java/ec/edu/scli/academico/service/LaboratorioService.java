package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.dto.laboratorio.LaboratorioRequest;
import ec.edu.scli.academico.dto.laboratorio.LaboratorioResponse;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LaboratorioService {

    LaboratorioResponse crear(LaboratorioRequest request);

    Page<LaboratorioResponse> listar(String texto, EstadoLaboratorio estado, Boolean activo, Pageable pageable);

    List<LaboratorioResponse> listarDisponibles();

    LaboratorioResponse obtenerPorId(UUID id);

    LaboratorioResponse actualizar(UUID id, LaboratorioRequest request);

    LaboratorioResponse cambiarEstado(UUID id, EstadoLaboratorio estado);

    // ---- Endpoints internos para Reservas Service ----

    LaboratorioDisponibilidadBaseResponse obtenerDisponibilidadBase(UUID id);

    ExisteResponse verificarExistencia(UUID id);
}
