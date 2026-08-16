package ec.edu.uteq.scli.auth_service.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}