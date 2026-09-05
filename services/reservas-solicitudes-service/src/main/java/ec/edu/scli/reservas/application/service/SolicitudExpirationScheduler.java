package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Expira solicitudes pendientes que superan la ventana configurable. */
@Component
public class SolicitudExpirationScheduler {
    private final SolicitudReservaRepositoryPort solicitudes; private final long graceMinutes;
    public SolicitudExpirationScheduler(SolicitudReservaRepositoryPort solicitudes,
            @Value("${app.reservas.expiration-grace-minutes:1440}") long graceMinutes) { this.solicitudes = solicitudes; this.graceMinutes = graceMinutes; }
    @Scheduled(fixedDelayString = "${app.reservas.expiration-check-ms:60000}")
    @Transactional public void expire() {
        if (graceMinutes <= 0) return;
        Instant now = Instant.now();
        solicitudes.buscarPendientesAnterioresA(now.minus(graceMinutes, ChronoUnit.MINUTES)).forEach(s -> { s.setEstado(EstadoSolicitud.EXPIRADA); s.setActualizadaEn(now); solicitudes.guardar(s); });
    }
}
