package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.carrera.CarreraRequest;
import ec.edu.scli.academico.dto.carrera.CarreraResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CarreraService {

    CarreraResponse crear(CarreraRequest request);

    Page<CarreraResponse> listar(UUID facultadId, String codigo, String nombre, Boolean activo, Pageable pageable);

    List<CarreraResponse> listarPorFacultad(UUID facultadId);

    CarreraResponse obtenerPorId(UUID id);

    CarreraResponse actualizar(UUID id, CarreraRequest request);

    void eliminar(UUID id);
}
