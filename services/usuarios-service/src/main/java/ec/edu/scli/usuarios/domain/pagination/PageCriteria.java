package ec.edu.scli.usuarios.domain.pagination;

import java.util.List;

public record PageCriteria(
        int page,
        int size,
        List<SortOrder> sort
) {
}
