package ec.edu.scli.academico.dto.equipo;

import ec.edu.scli.academico.enums.EstadoEquipo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EquipoResponse(

        UUID id,

        UUID laboratorioId,

        UUID tipoEquipoId,

        String codigoInventario,

        String numeroSerie,

        String marca,

        String modelo,

        String procesador,

        String memoriaRam,

        String almacenamiento,

        String direccionIp,

        String direccionMac,

        EstadoEquipo estado,

        String observacion,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
