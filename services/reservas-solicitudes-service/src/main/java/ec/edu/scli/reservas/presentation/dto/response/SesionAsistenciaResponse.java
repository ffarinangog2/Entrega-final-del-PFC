package ec.edu.scli.reservas.presentation.dto.response;
import java.time.*; import java.util.UUID;
public record SesionAsistenciaResponse(UUID id, UUID reservaId, UUID bloqueId, LocalDate fechaClase,
        Instant abiertaEn, Instant expiraEn, Instant cerradaEn, String estado, String token,
        String temaActividad, String observacionUso, UUID carreraId, UUID periodoId, Integer nivel,
        UUID materiaId, UUID docenteId, UUID laboratorioId, UUID pisoId, String diaSemana,
        LocalTime horaInicio, LocalTime horaFin, long esperados, long presentes, long ausentes) {}
