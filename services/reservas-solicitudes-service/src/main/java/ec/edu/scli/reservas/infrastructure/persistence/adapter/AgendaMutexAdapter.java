package ec.edu.scli.reservas.infrastructure.persistence.adapter;

import ec.edu.scli.reservas.domain.port.out.AgendaMutexPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class AgendaMutexAdapter implements AgendaMutexPort {
    private final JdbcTemplate jdbcTemplate;

    public AgendaMutexAdapter(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public void bloquear(UUID laboratorioId, LocalDate fecha) {
        jdbcTemplate.update("""
                INSERT INTO mutex_agenda (laboratorio_id, fecha)
                VALUES (?, ?) ON CONFLICT (laboratorio_id, fecha) DO NOTHING
                """, laboratorioId, fecha);
        jdbcTemplate.queryForObject("""
                SELECT version FROM mutex_agenda
                WHERE laboratorio_id = ? AND fecha = ? FOR UPDATE
                """, Long.class, laboratorioId, fecha);
    }
}
