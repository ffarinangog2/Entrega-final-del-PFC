package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.facultad.FacultadRequest;
import ec.edu.scli.academico.dto.facultad.FacultadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FacultadService {

    FacultadResponse crear(FacultadRequest request);

    Page<FacultadResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable);

    FacultadResponse obtenerPorId(UUID id);

    FacultadResponse actualizar(UUID id, FacultadRequest request);

    FacultadResponse cambiarEstado(UUID id, Boolean activo);

    void eliminar(UUID id);
}
