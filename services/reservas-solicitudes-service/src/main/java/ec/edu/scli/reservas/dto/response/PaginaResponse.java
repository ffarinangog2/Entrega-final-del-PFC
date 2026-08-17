package ec.edu.scli.reservas.dto.response;

import java.util.List;

/** Representación genérica de una página de resultados. */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {
}
