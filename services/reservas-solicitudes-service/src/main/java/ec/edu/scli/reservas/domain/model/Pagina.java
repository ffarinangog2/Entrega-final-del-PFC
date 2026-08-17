package ec.edu.scli.reservas.domain.model;

import java.util.List;

/** Página independiente de Spring Data. */
public record Pagina<T>(List<T> contenido, int numero, int tamanio, long totalElementos,
                         int totalPaginas, boolean primera, boolean ultima) { }
