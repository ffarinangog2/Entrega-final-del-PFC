package ec.edu.scli.usuarios.domain.pagination;

public record SortOrder(
        String property,
        Direction direction
) {

    public enum Direction {
        ASC,
        DESC
    }
}
