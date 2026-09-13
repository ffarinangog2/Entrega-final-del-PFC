package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.AdscripcionInstitucionalEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdministradorRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.PerfilRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AsociacionRolRequest;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AsociacionRolResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AsociacionRolService {
    private final PerfilRepository perfiles;
    private final AdministradorRepository administradores;
    private final AdscripcionInstitucionalRepository adscripciones;
    private final EstudianteRepository estudiantes;

    public AsociacionRolService(PerfilRepository perfiles, AdministradorRepository administradores,
            AdscripcionInstitucionalRepository adscripciones, EstudianteRepository estudiantes) {
        this.perfiles = perfiles;
        this.administradores = administradores;
        this.adscripciones = adscripciones;
        this.estudiantes = estudiantes;
    }

    @Transactional(readOnly = true)
    public AsociacionRolResponse consultar(UUID perfilId) {
        if (!perfiles.existsById(perfilId)) throw new IllegalArgumentException("Perfil institucional no encontrado.");
        UUID pisoId = administradores.findByPerfilId(perfilId).filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .map(Administrador::getPisoId).orElse(null);
        UUID carreraId = adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilId).stream()
                .filter(item -> item.getTipoAmbito() == TipoAmbitoInstitucional.CARRERA && item.isActivo())
                .map(AdscripcionInstitucionalEntity::getAmbitoId).findFirst().orElse(null);
        return new AsociacionRolResponse(pisoId, carreraId);
    }

    @Transactional
    public void asociar(UUID perfilId, AsociacionRolRequest request) {
        Perfil perfil = perfiles.findById(perfilId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil institucional no encontrado."));
        String rol = request.rol().trim().toUpperCase(Locale.ROOT);
        if ("ADMINISTRADOR_PISO".equals(rol) && request.pisoId() == null) {
            throw new IllegalArgumentException("Debe seleccionar un piso.");
        }
        if ("COORDINADOR".equals(rol) && request.carreraId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una carrera.");
        }

        administradores.findByPerfilId(perfilId).ifPresent(admin -> {
            admin.setActivo("ADMINISTRADOR_PISO".equals(rol) || "ADMINISTRADOR".equals(rol));
            admin.setPisoId("ADMINISTRADOR_PISO".equals(rol) ? request.pisoId() : null);
            administradores.save(admin);
        });
        adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilId).forEach(item -> {
            item.setActivo(false);
            adscripciones.save(item);
        });
        estudiantes.findByPerfilId(perfilId).ifPresent(item -> { item.setActivo("ESTUDIANTE".equals(rol)); estudiantes.save(item); });

        if ("ADMINISTRADOR_PISO".equals(rol) && administradores.findByPerfilId(perfilId).isEmpty()) {
            Administrador admin = new Administrador();
            admin.setPerfil(perfil);
            admin.setCodigoAdministrador("ADM-" + perfilId.toString().substring(0, 8).toUpperCase(Locale.ROOT));
            admin.setCargo("Administrador de piso");
            admin.setPisoId(request.pisoId());
            admin.setActivo(true);
            administradores.save(admin);
        } else if ("COORDINADOR".equals(rol)) {
            AdscripcionInstitucionalEntity adscripcion = adscripciones
                    .findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilId).stream()
                    .filter(item -> item.getTipoAmbito() == TipoAmbitoInstitucional.CARRERA
                            && request.carreraId().equals(item.getAmbitoId()))
                    .findFirst().orElseGet(AdscripcionInstitucionalEntity::new);
            adscripcion.setPerfil(perfil);
            adscripcion.setTipoAmbito(TipoAmbitoInstitucional.CARRERA);
            adscripcion.setAmbitoId(request.carreraId());
            adscripcion.setActivo(true);
            adscripciones.save(adscripcion);
        } else if ("ESTUDIANTE".equals(rol) && estudiantes.findByPerfilId(perfilId).isEmpty()) {
            ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante estudiante = new ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante();
            estudiante.setPerfil(perfil); estudiante.setMatricula("SCLI-" + perfilId.toString().substring(0, 8).toUpperCase(Locale.ROOT)); estudiante.setActivo(true);
            estudiantes.save(estudiante);
        }
    }
}
