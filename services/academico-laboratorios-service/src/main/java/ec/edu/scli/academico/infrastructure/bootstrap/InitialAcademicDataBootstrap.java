package ec.edu.scli.academico.infrastructure.bootstrap;

import ec.edu.scli.academico.infrastructure.persistence.entity.*;
import ec.edu.scli.academico.infrastructure.persistence.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.initial-data.enabled", havingValue = "true")
public class InitialAcademicDataBootstrap implements ApplicationRunner {
    public static final UUID CAMPUS_ID = UUID.fromString("33000000-0000-0000-0000-000000000001");
    public static final UUID BLOCK_ID = UUID.fromString("34000000-0000-0000-0000-000000000001");
    public static final UUID FLOOR_1_ID = UUID.fromString("35000000-0000-0000-0000-000000000001");
    public static final UUID FLOOR_2_ID = UUID.fromString("35000000-0000-0000-0000-000000000002");
    public static final UUID FACULTY_ID = UUID.fromString("36000000-0000-0000-0000-000000000001");
    public static final UUID CAREER_1_ID = UUID.fromString("37000000-0000-0000-0000-000000000001");
    public static final UUID CAREER_2_ID = UUID.fromString("37000000-0000-0000-0000-000000000002");

    private final CampusJpaRepository campuses; private final BloqueJpaRepository blocks;
    private final PisoJpaRepository floors; private final FacultadJpaRepository faculties;
    private final CarreraJpaRepository careers;

    public InitialAcademicDataBootstrap(CampusJpaRepository campuses, BloqueJpaRepository blocks,
            PisoJpaRepository floors, FacultadJpaRepository faculties, CarreraJpaRepository careers) {
        this.campuses=campuses; this.blocks=blocks; this.floors=floors; this.faculties=faculties; this.careers=careers;
    }

    @Override @Transactional public void run(ApplicationArguments args) {
        if (!campuses.existsById(CAMPUS_ID)) { var e=new CampusEntity(); e.setId(CAMPUS_ID);e.setCodigo("CAMPUS-01");e.setNombre("Campus Institucional");e.setActivo(true);campuses.save(e); }
        if (!blocks.existsById(BLOCK_ID)) { var e=new BloqueEntity();e.setId(BLOCK_ID);e.setCampusId(CAMPUS_ID);e.setCodigo("BLOQUE-LAB");e.setNombre("Bloque de Laboratorios");e.setActivo(true);blocks.save(e); }
        createFloor(FLOOR_1_ID,1,"Planta baja"); createFloor(FLOOR_2_ID,2,"Primer piso");
        if (!faculties.existsById(FACULTY_ID)) { var e=new FacultadEntity();e.setId(FACULTY_ID);e.setCodigo("FAC-TEC");e.setNombre("Facultad de Ciencias y Tecnología");e.setActivo(true);faculties.save(e); }
        createCareer(CAREER_1_ID,"CAR-SW","Ingeniería de Software"); createCareer(CAREER_2_ID,"CAR-TI","Tecnologías de la Información");
    }
    private void createFloor(UUID id,int number,String description){if(floors.existsById(id))return;var e=new PisoEntity();e.setId(id);e.setBloqueId(BLOCK_ID);e.setNumero(number);e.setDescripcion(description);e.setActivo(true);floors.save(e);}
    private void createCareer(UUID id,String code,String name){if(careers.existsById(id))return;var e=new CarreraEntity();e.setId(id);e.setFacultadId(FACULTY_ID);e.setCodigo(code);e.setNombre(name);e.setActivo(true);careers.save(e);}
}
