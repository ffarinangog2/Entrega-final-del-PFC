package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.application.usecase.PerfilService;
import ec.edu.scli.usuarios.infrastructure.client.AuthAdminClient;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.presentation.dto.usuarios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsuarioInstitucionalAdminService {
    private final PerfilService perfiles;
    private final AsociacionRolService asociaciones;
    private final AuthAdminClient auth;
    private final CompensacionAuthTransaccional compensaciones;

    public UsuarioInstitucionalAdminService(PerfilService perfiles, AsociacionRolService asociaciones,
            AuthAdminClient auth, CompensacionAuthTransaccional compensaciones) {
        this.perfiles = perfiles;
        this.asociaciones = asociaciones;
        this.auth = auth;
        this.compensaciones = compensaciones;
    }

    @Transactional
    public PerfilResponse crear(UsuarioInstitucionalCreateRequest request) {
        PerfilResponse perfil = perfiles.crear(request.perfil());
        asociaciones.asociar(perfil.id(), asociacion(request.rol(), request.pisoId(), request.carreraId()));
        AuthUsuarioResponse credencial = auth.crear(new AuthUsuarioCreateRequest(perfil.id(), request.username(), request.email(),
                request.passwordInicial(), request.rol()));
        compensaciones.registrarCreacion(credencial);
        return perfil;
    }

    @Transactional
    public PerfilResponse actualizar(UUID perfilId, UsuarioInstitucionalUpdateRequest request) {
        AuthUsuarioResponse estadoAuthAnterior = auth.obtener(request.authId());
        PerfilResponse perfil = perfiles.actualizar(perfilId, request.perfil());
        asociaciones.asociar(perfilId, asociacion(request.rol(), request.pisoId(), request.carreraId()));
        if (!request.activo()) perfiles.cambiarEstado(perfilId, false);
        else if (!Boolean.TRUE.equals(perfil.activo())) perfiles.cambiarEstado(perfilId, true);
        auth.actualizar(request.authId(), new AuthUsuarioUpdateRequest(
                request.username(), request.email(), request.rol(), request.activo()));
        compensaciones.registrarRestauracion(estadoAuthAnterior);
        return perfiles.obtenerPorId(perfilId);
    }

    private AsociacionRolRequest asociacion(String rol, UUID pisoId, UUID carreraId) {
        return new AsociacionRolRequest(rol, pisoId, carreraId);
    }
}
