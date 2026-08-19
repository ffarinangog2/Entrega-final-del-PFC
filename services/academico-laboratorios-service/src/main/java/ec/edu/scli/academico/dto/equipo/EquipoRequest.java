package ec.edu.scli.academico.dto.equipo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EquipoRequest(

        @NotNull(message = "El laboratorio es obligatorio")
        UUID laboratorioId,

        @NotNull(message = "El tipo de equipo es obligatorio")
        UUID tipoEquipoId,

        @NotBlank(message = "El código de inventario es obligatorio")
        @Size(max = 30, message = "El código de inventario no puede superar 30 caracteres")
        String codigoInventario,

        @Size(max = 60, message = "El número de serie no puede superar 60 caracteres")
        String numeroSerie,

        @Size(max = 60)
        String marca,

        @Size(max = 60)
        String modelo,

        @Size(max = 100)
        String procesador,

        @Size(max = 30)
        String memoriaRam,

        @Size(max = 30)
        String almacenamiento,

        @Size(max = 45)
        String direccionIp,

        @Size(max = 17)
        String direccionMac,

        @Size(max = 2000)
        String observacion

) {
}
