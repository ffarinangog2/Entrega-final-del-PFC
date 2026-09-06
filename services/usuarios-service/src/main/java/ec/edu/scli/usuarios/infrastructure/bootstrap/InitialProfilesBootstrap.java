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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.initial-data.enabled", havingValue = "true")
public class InitialProfilesBootstrap implements ApplicationRunner {
    private final PerfilRepository perfiles;
    private final AdministradorRepository administradores;
    private final DocenteRepository docentes;
    private final EstudianteRepository estudiantes;
    private final ContextoAcademicoEstudianteRepository contextos;
    private final AdscripcionInstitucionalRepository adscripciones;
    private final EntityManager entityManager;
    private final HmacIdentificacionService hmac;
    private final List<UUID> pisos;
    private final List<UUID> carreras;
    private final UUID periodoActualId;

    public InitialProfilesBootstrap(PerfilRepository perfiles, AdministradorRepository administradores,
            DocenteRepository docentes, EstudianteRepository estudiantes, ContextoAcademicoEstudianteRepository contextos,
            AdscripcionInstitucionalRepository adscripciones, EntityManager entityManager,
            HmacIdentificacionService hmac,
            @Value("${app.initial-data.piso-ids:}") String pisoIds,
            @Value("${app.initial-data.carrera-ids:}") String carreraIds,
            @Value("${app.initial-data.periodo-id:3f000000-0000-0000-0000-000000000001}") UUID periodoActualId) {
        this.perfiles = perfiles; this.administradores = administradores; this.docentes = docentes;
        this.estudiantes = estudiantes; this.contextos = contextos; this.adscripciones = adscripciones; this.entityManager = entityManager;
        this.hmac = hmac;
        this.pisos = parseIds(pisoIds); this.carreras = parseIds(carreraIds);
        this.periodoActualId = periodoActualId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (pisos.size() < 4 || carreras.size() < 2) {
            throw new IllegalStateException("INITIAL_PISO_IDS requiere cuatro pisos e INITIAL_CARRERA_IDS dos carreras existentes");
        }
        for (int index = 1; index <= 10; index++) createProfile(index);
        createAdmin(1, null, "ADM-GLOBAL-01", "Administración global");
        createAdmin(2, null, "ADM-GLOBAL-02", "Administración global");
        createAdmin(3, pisos.get(0), "ADM-PISO-01", "Administración de piso");
        createAdmin(4, pisos.get(1), "ADM-PISO-02", "Administración de piso");
        createNamedProfile("adminpiso.03", "Marina", "Vera Cedeño");
        createNamedProfile("adminpiso.04", "Esteban", "Mora Loor");
        createNamedAdmin("adminpiso.03", pisos.get(2), "ADM-PISO-03");
        createNamedAdmin("adminpiso.04", pisos.get(3), "ADM-PISO-04");
        createAffiliation(5, carreras.get(0)); createAffiliation(6, carreras.get(1));
        createTeacher(7); createTeacher(8);
        Estudiante softwareStudent = createStudent(9, carreras.get(0));
        Estudiante telematicsStudent = createStudent(10, carreras.get(1));
        createStudentContext(softwareStudent, carreras.get(0), 1);
        createStudentContext(telematicsStudent, carreras.get(1), 1);
        createIntegralTeachers();
        createIntegralStudents();
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

    private Estudiante createStudent(int number, UUID careerId) {
        UUID profileId = profileId(number);
        var existing = estudiantes.findByPerfilId(profileId); if (existing.isPresent()) return existing.get();
        Estudiante student = new Estudiante(); student.setPerfil(perfiles.getReferenceById(profileId));
        student.setMatricula("EST-LAB-" + String.format("%02d", number - 8)); student.setCarreraId(careerId);
        student.setSemestre(1); student.setActivo(true); return estudiantes.save(student);
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

    private void createIntegralTeachers() {
        String[] names = {"Andrea|Mendoza Ruiz", "Carlos|Villacís Mena", "Daniela|Torres Zambrano",
                "Felipe|Cedeño Paz", "Gabriela|Moreira Vélez", "Hugo|Santana Ortiz",
                "Isabel|Navarrete Cruz", "Javier|Ponce Alcívar", "Karen|Delgado Vera",
                "Luis|Salazar Mora", "Mónica|Reyes Loor", "Nicolás|Cabrera Soto",
                "Paola|Chávez Mero", "Ricardo|Vega Andrade", "Sofía|Ramos Cárdenas",
                "Tomás|Guerrero León", "Valeria|Macías Bravo", "Xavier|Palacios Rivas"};
        for (int index = 0; index < names.length; index++) {
            int number = index + 3;
            String username = "docente.%02d".formatted(number);
            String[] name = names[index].split("\\|");
            createNamedProfile(username, name[0], name[1]);
            createNamedTeacher(username, "DOC-%03d".formatted(number));
            createNamedAffiliation(username, carreras.get(index % 2));
            if (number % 5 == 0) createNamedAffiliation(username, carreras.get((index + 1) % 2));
        }
        createAffiliation(7, carreras.get(0));
        createAffiliation(8, carreras.get(1));
    }

    private void createIntegralStudents() {
        String[] firstNames = {"Adriana", "Bruno", "Camila", "Diego", "Elena", "Fernando", "Gloria", "Henry", "Irene", "Jorge"};
        String[] lastNames = {"Alcívar Mena", "Bravo Cedeño", "Castro Loor", "Díaz Mero", "Espinoza Mora",
                "Flores Paz", "García Ruiz", "Hidalgo Soto", "Intriago Vera", "Jaramillo Vélez"};
        for (int careerIndex = 0; careerIndex < 2; careerIndex++) {
            String career = careerIndex == 0 ? "software" : "telematica";
            for (int level = 1; level <= 10; level++) {
                for (int student = 1; student <= 10; student++) {
                    if (level == 1 && student == 1) continue;
                    String username = "estudiante.%s.%02d.%02d".formatted(career, level, student);
                    createNamedProfile(username, firstNames[student - 1], lastNames[(student + level + careerIndex) % lastNames.length]);
                    Estudiante created = createNamedStudent(username, careerIndex, level, student);
                    createStudentContext(created, carreras.get(careerIndex), level);
                }
            }
        }
    }

    private void createNamedProfile(String username, String names, String lastNames) {
        UUID id = stableId("profile:" + username);
        if (perfiles.existsById(id)) return;
        Perfil profile = new Perfil();
        profile.setId(id);
        String identification = "09" + String.format("%010d", Integer.toUnsignedLong(username.hashCode()));
        profile.setIdentificacion(identification);
        profile.setIdentificacionHash(hmac.calcularHash(identification));
        profile.setNombres(names); profile.setApellidos(lastNames);
        profile.setEmailInstitucional(username + "@scli.edu.ec"); profile.setActivo(true);
        entityManager.persist(profile);
    }

    private void createNamedAdmin(String username, UUID floorId, String code) {
        UUID profileId = stableId("profile:" + username);
        var existing = administradores.findByPerfilId(profileId);
        if (existing.isPresent()) {
            Administrador admin = existing.get();
            if (!floorId.equals(admin.getPisoId()) || !Boolean.TRUE.equals(admin.getActivo())) {
                admin.setPisoId(floorId); admin.setActivo(true); administradores.save(admin);
            }
            return;
        }
        Administrador admin = new Administrador(); admin.setId(stableId("admin:" + username));
        admin.setPerfil(perfiles.getReferenceById(profileId)); admin.setCodigoAdministrador(code);
        admin.setCargo("Administración de piso"); admin.setPisoId(floorId); admin.setActivo(true);
        administradores.save(admin);
    }

    private void createNamedTeacher(String username, String code) {
        UUID profileId = stableId("profile:" + username);
        if (docentes.existsByPerfilId(profileId)) return;
        Docente teacher = new Docente(); teacher.setId(stableId("teacher:" + username));
        teacher.setPerfil(perfiles.getReferenceById(profileId)); teacher.setCodigoDocente(code);
        teacher.setTituloAcademico("Magíster"); teacher.setDepartamento("Tecnologías de la Información");
        teacher.setTipoContrato("Tiempo completo"); teacher.setDedicacion("40 horas"); teacher.setActivo(true);
        docentes.save(teacher);
    }

    private Estudiante createNamedStudent(String username, int careerIndex, int level, int number) {
        UUID profileId = stableId("profile:" + username);
        return estudiantes.findByPerfilId(profileId).orElseGet(() -> {
            Estudiante value = new Estudiante(); value.setId(stableId("student:" + username));
            value.setPerfil(perfiles.getReferenceById(profileId));
            value.setMatricula("%s-%02d-%02d".formatted(careerIndex == 0 ? "SW" : "TEL", level, number));
            value.setCarreraId(carreras.get(careerIndex)); value.setSemestre(level); value.setActivo(true);
            return estudiantes.save(value);
        });
    }

    private void createStudentContext(Estudiante student, UUID careerId, int level) {
        if (student == null || student.getId() == null) throw new IllegalStateException("Estudiante inicial sin identificador persistido");
        if (contextos.findByEstudianteIdAndPeriodoId(student.getId(), periodoActualId).isPresent()) return;
        ContextoAcademicoEstudianteEntity context = new ContextoAcademicoEstudianteEntity();
        context.setId(stableId("context:" + student.getId() + ":" + periodoActualId));
        context.setEstudianteId(student.getId()); context.setCarreraId(careerId);
        context.setPeriodoId(periodoActualId); context.setNivel(level); context.setActivo(true);
        contextos.save(context);
    }

    private void createNamedAffiliation(String username, UUID careerId) {
        UUID profileId = stableId("profile:" + username);
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
    private static UUID stableId(String value) { return UUID.nameUUIDFromBytes(("scli-integral-test:" + value).getBytes(StandardCharsets.UTF_8)); }
    private static String username(int number) { return List.of("administrador.facultad01", "administrador.facultad02", "adminpiso.01", "adminpiso.02", "coordinacion.carrera01", "coordinacion.carrera02", "docente.lab01", "docente.lab02", "estudiante.lab01", "estudiante.lab02").get(number - 1); }
}
