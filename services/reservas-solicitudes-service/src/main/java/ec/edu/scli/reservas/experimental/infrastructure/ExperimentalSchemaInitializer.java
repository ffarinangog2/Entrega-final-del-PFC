package ec.edu.scli.reservas.experimental.infrastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.experimental.arbiter.enabled", havingValue = "true")
public class ExperimentalSchemaInitializer {
    private final JdbcTemplate jdbc;
    public ExperimentalSchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @PostConstruct
    void initialize() {
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS scli_experimental");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS scli_experimental.slot_versions (
                run_id STRING NOT NULL, equipment_id UUID NOT NULL, starts_at TIMESTAMPTZ NOT NULL,
                ends_at TIMESTAMPTZ NOT NULL, version INT8 NOT NULL DEFAULT 0,
                PRIMARY KEY (run_id, equipment_id, starts_at, ends_at))""");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS scli_experimental.equipment_mutex (
                run_id STRING NOT NULL, equipment_id UUID NOT NULL, starts_at TIMESTAMPTZ NOT NULL,
                ends_at TIMESTAMPTZ NOT NULL, version INT8 NOT NULL DEFAULT 0,
                PRIMARY KEY (run_id, equipment_id, starts_at, ends_at))""");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS scli_experimental.allocations (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(), request_id STRING NOT NULL,
                run_id STRING NOT NULL, equipment_id UUID NOT NULL, laboratorio_id UUID NOT NULL,
                agente_id UUID NOT NULL, starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL,
                status STRING NOT NULL, version INT8 NOT NULL, strategy STRING NOT NULL,
                reason STRING, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                UNIQUE (run_id, request_id))""");
    }
}
