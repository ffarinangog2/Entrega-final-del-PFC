package ec.edu.scli.reservas.experimental.infrastructure;

import ec.edu.scli.reservas.experimental.domain.SolicitudArbitraje;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JdbcExperimentalAllocationStoreTest {
    private JdbcTemplate jdbc;
    private JdbcExperimentalAllocationStore store;
    private final SolicitudArbitraje solicitud = new SolicitudArbitraje("run", "req", UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-09-07T08:00:00Z"),
            Instant.parse("2026-09-07T10:00:00Z"));

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
        store = new JdbcExperimentalAllocationStore(jdbc, transactions);
    }

    @Test
    void directaInsertaSinConsultarConflictos() {
        assertEquals("CONFIRMED", store.directa(solicitud, "s0").estado());
        verify(jdbc).update(contains("INSERT INTO scli_experimental.allocations"), any(Object[].class));
    }

    @Test
    void directaConvierteInstantesATimestampParaCompatibilidadJdbc() {
        store.directa(solicitud, "s0");
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT INTO scli_experimental.allocations"), captor.capture());
        Object[] args = captor.getValue();
        // starts_at (idx 5), ends_at (idx 6) y created_at (idx 9) deben ser java.sql.Timestamp para PostgreSQL/CockroachDB
        assertInstanceOf(Timestamp.class, args[5], "starts_at debe ser Timestamp para evitar PSQLException");
        assertInstanceOf(Timestamp.class, args[6], "ends_at debe ser Timestamp para evitar PSQLException");
        assertInstanceOf(Timestamp.class, args[9], "created_at debe ser Timestamp para evitar PSQLException");
        assertFalse(Arrays.stream(args).anyMatch(arg -> arg instanceof Instant),
                "Ningún argumento debe ser Instant directo en JDBC");
    }

    @Test
    void optimistaConfirmaConCompareAndSet() {
        when(jdbc.queryForObject(contains("SELECT version"), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.queryForObject(contains("SELECT count(*)"), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.update(contains("UPDATE scli_experimental.slot_versions"), any(Object[].class))).thenReturn(1);
        assertEquals("CONFIRMED", store.optimista(solicitud, "s1").estado());
    }

    @Test
    void optimistaRechazaCuandoCompareAndSetPierde() {
        when(jdbc.queryForObject(contains("SELECT version"), eq(Long.class), any(Object[].class))).thenReturn(2L);
        when(jdbc.queryForObject(contains("SELECT count(*)"), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.update(contains("UPDATE scli_experimental.slot_versions"), any(Object[].class))).thenReturn(0);
        assertEquals("OPTIMISTIC_CONFLICT", store.optimista(solicitud, "s1").motivo());
    }

    @Test
    void pesimistaBloqueaFranjaYRechazaConflictoReal() {
        when(jdbc.queryForObject(contains("SELECT version"), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.queryForObject(contains("SELECT count(*)"), eq(Long.class), any(Object[].class))).thenReturn(1L);
        assertEquals("REAL_CONFLICT", store.pesimista(solicitud, "s2").motivo());
        verify(jdbc).queryForObject(contains("FOR UPDATE"), eq(Long.class), any(Object[].class));
    }

    @Test
    void serializableConfirmaCuandoNoHaySolapamiento() {
        when(jdbc.queryForObject(contains("SELECT count(*)"), eq(Long.class), any(Object[].class))).thenReturn(0L);
        assertEquals("CONFIRMED", store.serializable(solicitud, "s4").estado());
    }

    @Test
    void serializableRechazaSolapamientoReal() {
        when(jdbc.queryForObject(contains("SELECT count(*)"), eq(Long.class), any(Object[].class))).thenReturn(1L);
        assertEquals("REAL_CONFLICT", store.serializable(solicitud, "s4").motivo());
    }

    @Test
    void inicializadorCreaSoloEsquemaExperimental() {
        new ExperimentalSchemaInitializer(jdbc).initialize();
        verify(jdbc, times(4)).execute(anyString());
        verify(jdbc).execute("CREATE SCHEMA IF NOT EXISTS scli_experimental");
    }
}
