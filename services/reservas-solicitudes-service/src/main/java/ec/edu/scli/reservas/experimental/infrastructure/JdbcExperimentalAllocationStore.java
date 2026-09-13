package ec.edu.scli.reservas.experimental.infrastructure;

import ec.edu.scli.reservas.experimental.domain.*;
import ec.edu.scli.reservas.experimental.port.ExperimentalAllocationStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.experimental.arbiter.enabled", havingValue = "true")
public class JdbcExperimentalAllocationStore implements ExperimentalAllocationStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactional;
    private final TransactionTemplate serializable;
    public JdbcExperimentalAllocationStore(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.transactional = new TransactionTemplate(transactions);
        this.serializable = new TransactionTemplate(transactions);
        this.serializable.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    }

    public ResultadoArbitraje directa(SolicitudArbitraje s, String strategy) {
        insert(s, strategy, 0); return confirmed(s, strategy, 0);
    }
    public ResultadoArbitraje optimista(SolicitudArbitraje s, String strategy) {
        try {
            return transactional.execute(status -> {
                jdbc.update("""
                        INSERT INTO scli_experimental.slot_versions(run_id,equipment_id,starts_at,ends_at,version)
                        VALUES (?,?,?,?,0) ON CONFLICT DO NOTHING""", s.runId(), s.equipmentId(), ts(s.inicio()), ts(s.fin()));
                Long version = jdbc.queryForObject("""
                        SELECT version FROM scli_experimental.slot_versions
                        WHERE run_id=? AND equipment_id=? AND starts_at=? AND ends_at=?""", Long.class,
                        s.runId(), s.equipmentId(), ts(s.inicio()), ts(s.fin()));
                if (conflicts(s)) return rejected(s, strategy, "REAL_CONFLICT", version == null ? 0 : version);
                int changed = jdbc.update("""
                        UPDATE scli_experimental.slot_versions SET version=version+1
                        WHERE run_id=? AND equipment_id=? AND starts_at=? AND ends_at=? AND version=?""",
                        s.runId(), s.equipmentId(), ts(s.inicio()), ts(s.fin()), version);
                if (changed != 1) return rejected(s, strategy, "OPTIMISTIC_CONFLICT", version == null ? 0 : version);
                long next = (version == null ? 0 : version) + 1; insert(s, strategy, next); return confirmed(s, strategy, next);
            });
        } catch (ConcurrencyFailureException exception) {
            return rejected(s, strategy, "OPTIMISTIC_CONFLICT", 0);
        }
    }
    public ResultadoArbitraje pesimista(SolicitudArbitraje s, String strategy) {
        return transactional.execute(status -> {
            jdbc.update("""
                    INSERT INTO scli_experimental.equipment_mutex(run_id,equipment_id,starts_at,ends_at,version)
                    VALUES (?,?,?,?,0) ON CONFLICT DO NOTHING""", s.runId(), s.equipmentId(), ts(s.inicio()), ts(s.fin()));
            jdbc.queryForObject("""
                    SELECT version FROM scli_experimental.equipment_mutex
                    WHERE run_id=? AND equipment_id=? AND starts_at=? AND ends_at=? FOR UPDATE""", Long.class,
                    s.runId(), s.equipmentId(), ts(s.inicio()), ts(s.fin()));
            if (conflicts(s)) return rejected(s, strategy, "REAL_CONFLICT", 0);
            insert(s, strategy, 0); return confirmed(s, strategy, 0);
        });
    }
    public ResultadoArbitraje serializable(SolicitudArbitraje s, String strategy) {
        int attempt = 0;
        while (true) {
            try {
                return serializable.execute(status -> {
                    if (conflicts(s)) return rejected(s, strategy, "REAL_CONFLICT", 0);
                    insert(s, strategy, 0); return confirmed(s, strategy, 0);
                });
            } catch (ConcurrencyFailureException exception) {
                if (++attempt >= 5) throw exception;
                try { Thread.sleep(25L << (attempt - 1)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
            }
        }
    }
    private boolean conflicts(SolicitudArbitraje s) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM scli_experimental.allocations
                WHERE run_id=? AND equipment_id=? AND status='CONFIRMED' AND starts_at < ? AND ends_at > ?""",
                Long.class, s.runId(), s.equipmentId(), ts(s.fin()), ts(s.inicio()));
        return count != null && count > 0;
    }
    private void insert(SolicitudArbitraje s, String strategy, long version) {
        jdbc.update("""
                INSERT INTO scli_experimental.allocations
                (request_id,run_id,equipment_id,laboratorio_id,agente_id,starts_at,ends_at,status,version,strategy,created_at)
                VALUES (?,?,?,?,?,?,?,'CONFIRMED',?,?,?)""", s.requestId(), s.runId(), s.equipmentId(),
                s.laboratorioId(), s.agenteId(), ts(s.inicio()), ts(s.fin()), version, strategy, ts(Instant.now()));
    }
    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
    private ResultadoArbitraje confirmed(SolicitudArbitraje s, String strategy, long version) {
        return new ResultadoArbitraje(s.runId(), s.requestId(), strategy, "CONFIRMED", null, version, null, null, null, Instant.now());
    }
    private ResultadoArbitraje rejected(SolicitudArbitraje s, String strategy, String reason, long version) {
        return new ResultadoArbitraje(s.runId(), s.requestId(), strategy, "REJECTED", reason, version, null, null, null, Instant.now());
    }
}
