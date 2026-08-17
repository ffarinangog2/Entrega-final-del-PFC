package ec.edu.scli.usuarios.domain.pagination;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(
                content.stream().map(mapper).toList(),
                totalElements,
                totalPages,
                page,
                size
        );
    }
}
