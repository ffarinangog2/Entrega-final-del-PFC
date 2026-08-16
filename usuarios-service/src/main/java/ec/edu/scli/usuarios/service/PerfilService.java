package ec.edu.scli.usuarios.service;

import ec.edu.scli.usuarios.dto.perfil.PerfilAuthResponse;
import ec.edu.scli.usuarios.dto.perfil.PerfilCreateRequest;
import ec.edu.scli.usuarios.dto.perfil.PerfilExistsResponse;
import ec.edu.scli.usuarios.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.dto.perfil.PerfilUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ec.edu.scli.usuarios.enums.TipoPerfil;
import java.util.UUID;

public interface PerfilService {

    PerfilResponse crear(PerfilCreateRequest request);

    Page<PerfilResponse> listar(
            String identificacion,
            String nombre,
            String email,
            TipoPerfil tipoPerfil,
            Boolean activo,
            Pageable pageable);

    PerfilResponse obtenerPorId(UUID id);

    PerfilResponse actualizar(UUID id, PerfilUpdateRequest request);

    PerfilResponse cambiarEstado(UUID id, Boolean activo);

    void eliminar(UUID id);

    PerfilExistsResponse verificarExistencia(UUID perfilId);

    PerfilAuthResponse obtenerParaAuth(UUID perfilId);
}