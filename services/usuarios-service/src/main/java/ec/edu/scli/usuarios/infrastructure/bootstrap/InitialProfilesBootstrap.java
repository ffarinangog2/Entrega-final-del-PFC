package ec.edu.scli.usuarios.infrastructure.bootstrap;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.*;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.*;
import ec.edu.scli.usuarios.infrastructure.security.HmacIdentificacionService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.initial-data.enabled", havingValue = "true")
public class InitialProfilesBootstrap implements ApplicationRunner {
    private final PerfilRepository perfiles;
    private final AdministradorRepository administradores;
    private final DocenteRepository docentes;
    private final EstudianteRepository estudiantes;
    private final AdscripcionInstitucionalRepository adscripciones;
    private final EntityManager entityManager;
    private final HmacIdentificacionService hmac;
    private final List<UUID> pisos;
    private final List<UUID> carreras;

    public InitialProfilesBootstrap(PerfilRepository perfiles, AdministradorRepository administradores,
            DocenteRepository docentes, EstudianteRepository estudiantes,
            AdscripcionInstitucionalRepository adscripciones, EntityManager entityManager,
            HmacIdentificacionService hmac,
            @Value("${app.initial-data.piso-ids:}") String pisoIds,
            @Value("${app.initial-data.carrera-ids:}") String carreraIds) {
        this.perfiles = perfiles; this.administradores = administradores; this.docentes = docentes;
        this.estudiantes = estudiantes; this.adscripciones = adscripciones; this.entityManager = entityManager;
        this.hmac = hmac;
        this.pisos = parseIds(pisoIds); this.carreras = parseIds(carreraIds);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (pisos.size() < 2 || carreras.size() < 2) {
            throw new IllegalStateException("INITIAL_PISO_IDS e INITIAL_CARRERA_IDS requieren al menos dos UUID existentes");
        }
        for (int index = 1; index <= 10; index++) createProfile(index);
        createAdmin(1, null, "ADM-GLOBAL-01", "Administración global");
        createAdmin(2, null, "ADM-GLOBAL-02", "Administración global");
        createAdmin(3, pisos.get(0), "ADM-PISO-01", "Administración de piso");
        createAdmin(4, pisos.get(1), "ADM-PISO-02", "Administración de piso");
        createAffiliation(5, carreras.get(0)); createAffiliation(6, carreras.get(1));
        createTeacher(7); createTeacher(8);
        createStudent(9, carreras.get(0)); createStudent(10, carreras.get(1));
    }

    private void createProfile(int number) {
        UUID id = profileId(number);
        if (perfiles.existsById(id)) return;
        Perfil profile = new Perfil();
        profile.setId(id);
        String identification = "099900" + String.format("%04d", number);
        profile.setIdentificacion(identification);
        profile.setIdentificacionHash(hmac.calcularHash(identification));
        profile.setNombres("Usuario"); profile.setApellidos("Institucional " + String.format("%02d", number));
        profile.setEmailInstitucional(username(number) + "@scli.local"); profile.setActivo(true);
        entityManager.persist(profile);
    }

    private void createAdmin(int number, UUID pisoId, String code, String position) {
        UUID profileId = profileId(number);
        var existente = administradores.findByPerfilId(profileId);
        if (existente.isPresent()) {
            var admin = existente.get();
            boolean modificado = false;
            if (!java.util.Objects.equals(admin.getPisoId(), pisoId)) {
                admin.setPisoId(pisoId);
                modificado = true;
            }
            if (!java.util.Objects.equals(admin.getCodigoAdministrador(), code)) {
                admin.setCodigoAdministrador(code);
                modificado = true;
            }
            if (!java.util.Objects.equals(admin.getCargo(), position)) {
                admin.setCargo(position);
                modificado = true;
            }
            if (!Boolean.TRUE.equals(admin.getActivo())) {
                admin.setActivo(true);
                modificado = true;
            }
            if (modificado) {
                administradores.save(admin);
            }
            return;
        }
        Administrador admin = new Administrador(); admin.setPerfil(perfiles.getReferenceById(profileId));
        admin.setCodigoAdministrador(code); admin.setCargo(position); admin.setPisoId(pisoId); admin.setActivo(true);
        administradores.save(admin);
    }

    private void createTeacher(int number) {
        UUID profileId = profileId(number); if (docentes.existsByPerfilId(profileId)) return;
        Docente teacher = new Docente(); teacher.setPerfil(perfiles.getReferenceById(profileId));
        teacher.setCodigoDocente("DOC-LAB-" + String.format("%02d", number - 6)); teacher.setDepartamento("Laboratorios");
        teacher.setActivo(true); docentes.save(teacher);
    }

    private void createStudent(int number, UUID careerId) {
        UUID profileId = profileId(number); if (estudiantes.existsByPerfilId(profileId)) return;
        Estudiante student = new Estudiante(); student.setPerfil(perfiles.getReferenceById(profileId));
        student.setMatricula("EST-LAB-" + String.format("%02d", number - 8)); student.setCarreraId(careerId);
        student.setSemestre(1); student.setActivo(true); estudiantes.save(student);
    }

    private void createAffiliation(int number, UUID careerId) {
        UUID profileId = profileId(number);
        boolean exists = adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(profileId).stream()
                .anyMatch(value -> value.getTipoAmbito() == TipoAmbitoInstitucional.CARRERA && value.getAmbitoId().equals(careerId));
        if (exists) return;
        AdscripcionInstitucionalEntity affiliation = new AdscripcionInstitucionalEntity();
        affiliation.setPerfil(perfiles.getReferenceById(profileId)); affiliation.setTipoAmbito(TipoAmbitoInstitucional.CARRERA);
        affiliation.setAmbitoId(careerId); affiliation.setActivo(true); adscripciones.save(affiliation);
    }

    private static List<UUID> parseIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).map(UUID::fromString).toList();
    }
    private static UUID profileId(int number) { return UUID.fromString("22000000-0000-0000-0000-0000000000" + String.format("%02d", number)); }
    private static String username(int number) { return List.of("administrador.facultad01", "administrador.facultad02", "adminpiso.01", "adminpiso.02", "coordinacion.carrera01", "coordinacion.carrera02", "docente.lab01", "docente.lab02", "estudiante.lab01", "estudiante.lab02").get(number - 1); }
}
