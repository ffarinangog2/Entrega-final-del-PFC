package ec.edu.scli.academico.application.facade.impl;

import ec.edu.scli.academico.application.facade.LaboratorioDetalleFacade;
import ec.edu.scli.academico.application.service.BloqueService;
import ec.edu.scli.academico.application.service.CampusService;
import ec.edu.scli.academico.application.service.EquipoService;
import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.application.service.PisoService;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueResponse;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioDetalleCompletoResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioResponse;
import ec.edu.scli.academico.presentation.dto.piso.PisoResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementación del Facade: coordina 5 servicios de aplicación
 * (Laboratorio, Piso, Bloque, Campus, Equipo) para construir la ficha
 * completa de un laboratorio en una sola operación simple.
 */
@Service
public class LaboratorioDetalleFacadeImpl implements LaboratorioDetalleFacade {

    private final LaboratorioService laboratorioService;
    private final PisoService pisoService;
    private final BloqueService bloqueService;
    private final CampusService campusService;
    private final EquipoService equipoService;

    public LaboratorioDetalleFacadeImpl(
            LaboratorioService laboratorioService,
            PisoService pisoService,
            BloqueService bloqueService,
            CampusService campusService,
            EquipoService equipoService
    ) {
        this.laboratorioService = laboratorioService;
        this.pisoService = pisoService;
        this.bloqueService = bloqueService;
        this.campusService = campusService;
        this.equipoService = equipoService;
    }

    @Override
    public LaboratorioDetalleCompletoResponse obtenerDetalleCompleto(UUID laboratorioId) {
        LaboratorioResponse laboratorio = laboratorioService.obtenerPorId(laboratorioId);
        PisoResponse piso = pisoService.obtenerPorId(laboratorio.pisoId());
        BloqueResponse bloque = bloqueService.obtenerPorId(piso.bloqueId());
        CampusResponse campus = campusService.obtenerPorId(bloque.campusId());
        List<EquipoResponse> equipos = equipoService.listarPorLaboratorio(laboratorioId);

        return new LaboratorioDetalleCompletoResponse(laboratorio, piso, bloque, campus, equipos);
    }
}