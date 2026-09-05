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
    private final ContextoAcademicoEstudianteService contextos;

    public UsuarioInstitucionalAdminService(PerfilService perfiles, AsociacionRolService asociaciones,
            AuthAdminClient auth, CompensacionAuthTransaccional compensaciones, ContextoAcademicoEstudianteService contextos) {
        this.perfiles = perfiles;
        this.asociaciones = asociaciones;
        this.auth = auth;
        this.compensaciones = compensaciones;
        this.contextos = contextos;
    }

    @Transactional
    public PerfilResponse crear(UsuarioInstitucionalCreateRequest request) {
        PerfilResponse perfil = perfiles.crear(request.perfil());
        asociaciones.asociar(perfil.id(), asociacion(request.rol(), request.pisoId(), request.carreraId()));
        asignarContextoEstudiante(perfil.id(),request.rol(),request.carreraId(),request.periodoId(),request.nivel());
        AuthUsuarioResponse credencial = auth.crear(new AuthUsuarioCreateRequest(perfil.id(), request.username(), request.email(),
                request.passwordInicial(), request.rol()));
        compensaciones.registrarCreacion(credencial);
        return perfil;
    }

    @Transactional
    public PerfilResponse actualizar(UUID perfilId, UsuarioInstitucionalUpdateRequest request) {
        PerfilResponse perfil = perfiles.actualizar(perfilId, request.perfil());
        asociaciones.asociar(perfilId, asociacion(request.rol(), request.pisoId(), request.carreraId()));
        asignarContextoEstudiante(perfilId,request.rol(),request.carreraId(),request.periodoId(),request.nivel());
        if (!request.activo()) perfiles.cambiarEstado(perfilId, false);
        else if (!Boolean.TRUE.equals(perfil.activo())) perfiles.cambiarEstado(perfilId, true);
        AuthUsuarioResponse estadoAuthAnterior = auth.obtener(request.authId());
        auth.actualizar(request.authId(), new AuthUsuarioUpdateRequest(
                request.username(), request.email(), request.rol(), request.activo()));
        compensaciones.registrarRestauracion(estadoAuthAnterior);
        return perfiles.obtenerPorId(perfilId);
    }

    private AsociacionRolRequest asociacion(String rol, UUID pisoId, UUID carreraId) {
        return new AsociacionRolRequest(rol, pisoId, carreraId);
    }
    private void asignarContextoEstudiante(UUID perfilId,String rol,UUID carreraId,UUID periodoId,Integer nivel){
        if(!"ESTUDIANTE".equalsIgnoreCase(rol)) return;
        if(carreraId==null||periodoId==null||nivel==null) throw new IllegalArgumentException("Debe seleccionar carrera, ciclo académico y nivel para el estudiante.");
        contextos.asignarPorPerfil(perfilId,new ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoAcademicoEstudianteRequest(carreraId,periodoId,nivel));
    }
}
