package ec.edu.scli.academico.infrastructure.bootstrap;

import ec.edu.scli.academico.enums.DiaSemana;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.infrastructure.persistence.entity.BloqueEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.CampusEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.CarreraEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.EquipoEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.FacultadEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.HorarioAcademicoEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.LaboratorioEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.MateriaEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.PeriodoLectivoEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.PisoEntity;
import ec.edu.scli.academico.infrastructure.persistence.entity.TipoEquipoEntity;
import ec.edu.scli.academico.infrastructure.persistence.repository.BloqueJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.CampusJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.CarreraJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.EquipoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.FacultadJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.HorarioAcademicoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.LaboratorioJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.MateriaJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.PeriodoLectivoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.PisoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.TipoEquipoJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.initial-data.enabled", havingValue = "true")
public class InitialAcademicDataBootstrap implements ApplicationRunner {
    public static final UUID CAMPUS_ID = id("33000000-0000-0000-0000-000000000001");
    public static final UUID BLOCK_ID = id("34000000-0000-0000-0000-000000000001");
    public static final UUID BLOCK_2_ID = id("34000000-0000-0000-0000-000000000002");
    public static final UUID FLOOR_1_ID = id("35000000-0000-0000-0000-000000000001");
    public static final UUID FLOOR_2_ID = id("35000000-0000-0000-0000-000000000002");
    public static final UUID FLOOR_3_ID = id("35000000-0000-0000-0000-000000000003");
    public static final UUID FLOOR_4_ID = id("35000000-0000-0000-0000-000000000004");
    public static final UUID FACULTY_ID = id("36000000-0000-0000-0000-000000000001");
    public static final UUID CAREER_1_ID = id("37000000-0000-0000-0000-000000000001");
    public static final UUID CAREER_2_ID = id("37000000-0000-0000-0000-000000000002");
    private static final UUID PERIOD_ACTIVE_ID = id("38000000-0000-0000-0000-000000000001");
    private static final UUID PERIOD_FINISHED_ID = id("38000000-0000-0000-0000-000000000002");

    private final CampusJpaRepository campuses; private final BloqueJpaRepository blocks;
    private final PisoJpaRepository floors; private final FacultadJpaRepository faculties;
    private final CarreraJpaRepository careers; private final PeriodoLectivoJpaRepository periods;
    private final MateriaJpaRepository subjects; private final LaboratorioJpaRepository labs;
    private final TipoEquipoJpaRepository equipmentTypes; private final EquipoJpaRepository equipment;
    private final HorarioAcademicoJpaRepository schedules;

    public InitialAcademicDataBootstrap(CampusJpaRepository campuses, BloqueJpaRepository blocks,
            PisoJpaRepository floors, FacultadJpaRepository faculties, CarreraJpaRepository careers,
            PeriodoLectivoJpaRepository periods, MateriaJpaRepository subjects, LaboratorioJpaRepository labs,
            TipoEquipoJpaRepository equipmentTypes, EquipoJpaRepository equipment,
            HorarioAcademicoJpaRepository schedules) {
        this.campuses = campuses; this.blocks = blocks; this.floors = floors; this.faculties = faculties;
        this.careers = careers; this.periods = periods; this.subjects = subjects; this.labs = labs;
        this.equipmentTypes = equipmentTypes; this.equipment = equipment; this.schedules = schedules;
    }

    @Override @Transactional public void run(ApplicationArguments args) {
        campusAndStructure(); facultyAndCareers(); periods(); subjects(); laboratoriesAndEquipment(); schedules();
    }

    private void campusAndStructure() {
        if (!campuses.existsById(CAMPUS_ID)) {
            CampusEntity e = new CampusEntity(); e.setId(CAMPUS_ID); e.setCodigo("CAMPUS-01");
            e.setNombre("Campus Institucional"); e.setActivo(true); campuses.save(e);
        }
        createBlock(BLOCK_ID, "BLOQUE-LAB-01", "Bloque de Laboratorios");
        createBlock(BLOCK_2_ID, "BLOQUE-LAB-02", "Bloque de Innovacion");
        createFloor(FLOOR_1_ID, BLOCK_ID, 0, "Planta Baja"); createFloor(FLOOR_2_ID, BLOCK_ID, 1, "Piso 1");
        createFloor(FLOOR_3_ID, BLOCK_2_ID, 2, "Piso 2"); createFloor(FLOOR_4_ID, BLOCK_2_ID, 3, "Piso 3");
    }

    private void facultyAndCareers() {
        if (!faculties.existsById(FACULTY_ID)) {
            FacultadEntity e = new FacultadEntity(); e.setId(FACULTY_ID); e.setCodigo("FAC-TEC");
            e.setNombre("Facultad de Ciencias y Tecnologia"); e.setActivo(true); faculties.save(e);
        }
        createCareer(CAREER_1_ID, "CAR-SW", "Ingenieria de Software");
        createCareer(CAREER_2_ID, "CAR-TI", "Tecnologias de la Informacion");
    }

    private void periods() {
        createPeriod(PERIOD_ACTIVE_ID, "2026-B", "Periodo Lectivo 2026-B", EstadoPeriodo.ACTIVO,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));
        createPeriod(PERIOD_FINISHED_ID, "2026-A", "Periodo Lectivo 2026-A", EstadoPeriodo.FINALIZADO,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 6, 30));
    }

    private void subjects() {
        createSubject(1, CAREER_1_ID, "SW-101", "Programacion I");
        createSubject(2, CAREER_1_ID, "SW-202", "Bases de Datos");
        createSubject(3, CAREER_1_ID, "SW-303", "Ingenieria de Software");
        createSubject(4, CAREER_2_ID, "TI-101", "Redes de Computadores");
        createSubject(5, CAREER_2_ID, "TI-202", "Sistemas Operativos");
        createSubject(6, CAREER_2_ID, "TI-303", "Seguridad Informatica");
    }

    private void laboratoriesAndEquipment() {
        UUID[] floorIds = {FLOOR_1_ID, FLOOR_1_ID, FLOOR_2_ID, FLOOR_2_ID,
            FLOOR_3_ID, FLOOR_3_ID, FLOOR_4_ID, FLOOR_4_ID};
        String[] names = {"Laboratorio de Software", "Laboratorio de Bases de Datos", "Laboratorio de Redes",
            "Laboratorio de Sistemas", "Laboratorio de Electronica", "Laboratorio Multimedia",
            "Laboratorio de Innovacion", "Laboratorio de Seguridad"};
        UUID[] labIds = new UUID[8];
        for (int i = 0; i < 8; i++) {
            labIds[i] = id(String.format("39000000-0000-0000-0000-%012d", i + 1));
            createLab(labIds[i], floorIds[i], "LAB-%02d".formatted(i + 1), names[i]);
        }
        UUID computer = createType(1, "COMPUTO", "Equipos de computo");
        UUID network = createType(2, "REDES", "Equipos de red");
        UUID measurement = createType(3, "MEDICION", "Instrumentos de medicion");
        for (int i = 0; i < 8; i++) {
            createEquipment(i + 1, labIds[i], i % 3 == 0 ? network : (i % 3 == 1 ? measurement : computer));
        }
    }

    private void schedules() {
        UUID[] labIds = new UUID[8];
        for (int i = 0; i < 8; i++) {
            labIds[i] = id(String.format("39000000-0000-0000-0000-%012d", i + 1));
        }
        for (int i = 0; i < 6; i++) {
            UUID scheduleId = id(String.format("3A000000-0000-0000-0000-%012d", i + 1));
            if (!schedules.existsById(scheduleId)) {
                HorarioAcademicoEntity e = new HorarioAcademicoEntity(); e.setId(scheduleId);
                e.setMateriaId(id(String.format("3B000000-0000-0000-0000-%012d", i + 1)));
                e.setPeriodoLectivoId(PERIOD_ACTIVE_ID); e.setLaboratorioId(labIds[i]);
                e.setDocenteId(id(String.format("22000000-0000-0000-0000-0000000000%02d", 7 + (i % 2))));
                e.setDiaSemana(DiaSemana.values()[i]); e.setHoraInicio(LocalTime.of(8 + i, 0));
                e.setHoraFin(LocalTime.of(10 + i, 0)); e.setParalelo("A"); e.setActivo(true); schedules.save(e);
            }
        }
    }

    private void createBlock(UUID id, String code, String name) {
        if (!blocks.existsById(id)) {
            BloqueEntity e = new BloqueEntity(); e.setId(id); e.setCampusId(CAMPUS_ID); e.setCodigo(code);
            e.setNombre(name); e.setActivo(true); blocks.save(e);
        }
    }

    private void createFloor(UUID id, UUID blockId, int number, String description) {
        if (!floors.existsById(id)) {
            PisoEntity e = new PisoEntity(); e.setId(id); e.setBloqueId(blockId); e.setNumero(number);
            e.setDescripcion(description); e.setActivo(true); floors.save(e);
        }
    }

    private void createCareer(UUID id, String code, String name) {
        if (!careers.existsById(id)) {
            CarreraEntity e = new CarreraEntity(); e.setId(id); e.setFacultadId(FACULTY_ID); e.setCodigo(code);
            e.setNombre(name); e.setActivo(true); careers.save(e);
        }
    }

    private void createPeriod(UUID id, String code, String name, EstadoPeriodo state, LocalDate start, LocalDate end) {
        if (!periods.existsById(id)) {
            PeriodoLectivoEntity e = new PeriodoLectivoEntity(); e.setId(id); e.setCodigo(code); e.setNombre(name);
            e.setEstado(state); e.setFechaInicio(start); e.setFechaFin(end); periods.save(e);
        }
    }

    private void createSubject(int n, UUID careerId, String code, String name) {
        UUID id = id(String.format("3B000000-0000-0000-0000-%012d", n));
        if (!subjects.existsById(id)) {
            MateriaEntity e = new MateriaEntity(); e.setId(id); e.setCarreraId(careerId); e.setCodigo(code);
            e.setNombre(name); e.setNumeroHoras(64); e.setActivo(true); subjects.save(e);
        }
    }

    private void createLab(UUID id, UUID floorId, String code, String name) {
        if (!labs.existsById(id)) {
            LaboratorioEntity e = new LaboratorioEntity(); e.setId(id); e.setPisoId(floorId); e.setCodigo(code);
            e.setNombre(name); e.setCapacidad(30); e.setDescripcion("Espacio academico equipado");
            e.setEstado(EstadoLaboratorio.DISPONIBLE); e.setActivo(true); labs.save(e);
        }
    }

    private UUID createType(int n, String code, String name) {
        UUID id = id(String.format("3C000000-0000-0000-0000-%012d", n));
        if (!equipmentTypes.existsById(id)) {
            TipoEquipoEntity e = new TipoEquipoEntity(); e.setId(id); e.setCodigo(code); e.setNombre(name);
            e.setActivo(true); equipmentTypes.save(e);
        }
        return id;
    }

    private void createEquipment(int n, UUID labId, UUID typeId) {
        UUID id = id(String.format("3D000000-0000-0000-0000-%012d", n));
        if (!equipment.existsById(id)) {
            EquipoEntity e = new EquipoEntity(); e.setId(id); e.setLaboratorioId(labId);
            e.setTipoEquipoId(typeId); e.setCodigoInventario("INV-LAB-%03d".formatted(n));
            e.setNumeroSerie("SN-LAB-%03d".formatted(n)); e.setMarca("Generic"); e.setModelo("Academic");
            e.setEstado(EstadoEquipo.OPERATIVO); e.setActivo(true); equipment.save(e);
        }
    }
    private static UUID id(String value) { return UUID.fromString(value); }
}
