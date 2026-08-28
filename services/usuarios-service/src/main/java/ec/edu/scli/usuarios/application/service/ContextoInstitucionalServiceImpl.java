package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.application.usecase.ContextoInstitucionalService;
import ec.edu.scli.usuarios.domain.model.Administrador;
import ec.edu.scli.usuarios.domain.model.ContextoInstitucional;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.model.TipoPerfil;
import ec.edu.scli.usuarios.domain.port.AdministradorRepositoryPort;
import ec.edu.scli.usuarios.domain.port.AdscripcionInstitucionalRepositoryPort;
import ec.edu.scli.usuarios.domain.port.DocenteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.EstudianteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ContextoInstitucionalServiceImpl implements ContextoInstitucionalService {

    private final PerfilRepositoryPort perfiles;
    private final DocenteRepositoryPort docentes;
    private final EstudianteRepositoryPort estudiantes;
    private final AdministradorRepositoryPort administradores;
    private final AdscripcionInstitucionalRepositoryPort adscripciones;

    public ContextoInstitucionalServiceImpl(
            PerfilRepositoryPort perfiles,
            DocenteRepositoryPort docentes,
            EstudianteRepositoryPort estudiantes,
            AdministradorRepositoryPort administradores,
            AdscripcionInstitucionalRepositoryPort adscripciones) {
        this.perfiles = perfiles;
        this.docentes = docentes;
        this.estudiantes = estudiantes;
        this.administradores = administradores;
        this.adscripciones = adscripciones;
    }

    @Override
    @Transactional(readOnly = true)
    public ContextoInstitucional obtenerPorPerfilId(UUID perfilId) {
        Perfil perfil = perfiles.findById(perfilId).orElse(null);
        if (perfil == null) {
            return new ContextoInstitucional(
                    perfilId, false, false, List.of(), administradorAusente(), List.of());
        }

        List<TipoPerfil> tipos = new ArrayList<>();
        if (docentes.existsByPerfilId(perfilId)) tipos.add(TipoPerfil.DOCENTE);
        if (estudiantes.existsByPerfilId(perfilId)) tipos.add(TipoPerfil.ESTUDIANTE);

        Administrador administrador = administradores.findByPerfilId(perfilId).orElse(null);
        if (administrador != null) tipos.add(TipoPerfil.ADMINISTRADOR);

        return new ContextoInstitucional(
                perfilId,
                true,
                Boolean.TRUE.equals(perfil.getActivo()),
                List.copyOf(tipos),
                contextoAdministrador(perfil, administrador),
                List.copyOf(adscripciones.findByPerfilId(perfilId)));
    }

    private ContextoInstitucional.ContextoAdministrador contextoAdministrador(
            Perfil perfil, Administrador administrador) {
        if (administrador == null) return administradorAusente();
        boolean activo = Boolean.TRUE.equals(administrador.getActivo());
        boolean operativo = Boolean.TRUE.equals(perfil.getActivo())
                && activo
                && administrador.getPisoId() != null;
        return new ContextoInstitucional.ContextoAdministrador(
                true, activo, administrador.getPisoId(), administrador.getCargo(), operativo);
    }

    private ContextoInstitucional.ContextoAdministrador administradorAusente() {
        return new ContextoInstitucional.ContextoAdministrador(false, false, null, null, false);
    }
}
