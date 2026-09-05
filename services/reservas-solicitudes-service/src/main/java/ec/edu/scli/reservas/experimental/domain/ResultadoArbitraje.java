package ec.edu.scli.reservas.experimental.domain;

import java.time.Instant;

public record ResultadoArbitraje(String runId, String requestId, String estrategia,
        String estado, String motivo, long version, Integer nodeId, Integer leaderId,
        Long lamport, Instant decididoEn) { }
