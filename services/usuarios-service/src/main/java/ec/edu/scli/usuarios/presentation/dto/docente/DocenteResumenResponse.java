package ec.edu.scli.usuarios.presentation.dto.docente;
import java.util.UUID;
public record DocenteResumenResponse(UUID id,String nombres,String apellidos,String codigoDocente) { }
