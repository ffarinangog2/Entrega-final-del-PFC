package ec.edu.scli.reservas.client.dto;
import java.time.LocalDate; import java.util.UUID;
public record PeriodoExternoResponse(UUID id,String codigo,String nombre,LocalDate fechaInicio,LocalDate fechaFin,String estado,String ppaCodigo,String ppaNombre,Integer cicloAcademico){}
