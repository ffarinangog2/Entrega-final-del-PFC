package ec.edu.scli.reservas.presentation.dto.response;

import java.util.List;

public record PaginaNotificacionesResponse(List<NotificacionInternaResponse> content,
        int number, int size, long totalElements, int totalPages) { }
