package ec.edu.scli.usuarios.service;

import ec.edu.scli.usuarios.dto.administrador.AdministradorRequest;
import ec.edu.scli.usuarios.dto.administrador.AdministradorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdministradorService {

    AdministradorResponse crear(AdministradorRequest request);

    Page<AdministradorResponse> listar(Pageable pageable);

    AdministradorResponse obtenerPorId(UUID id);

    AdministradorResponse actualizar(
            UUID id,
            AdministradorRequest request
    );
}