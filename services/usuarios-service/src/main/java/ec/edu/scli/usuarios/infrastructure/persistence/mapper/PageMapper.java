package ec.edu.scli.usuarios.infrastructure.persistence.mapper;

import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.pagination.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

public final class PageMapper {

    private PageMapper() {
    }

    public static Pageable toPageable(PageCriteria criteria) {
        if (criteria == null) {
            return Pageable.unpaged();
        }

        Sort sort = Sort.unsorted();

        if (criteria.sort() != null && !criteria.sort().isEmpty()) {
            sort = Sort.by(criteria.sort().stream()
                    .map(order -> new Sort.Order(
                            order.direction() == SortOrder.Direction.DESC
                                    ? Sort.Direction.DESC
                                    : Sort.Direction.ASC,
                            order.property()
                    ))
                    .toList());
        }

        return PageRequest.of(criteria.page(), criteria.size(), sort);
    }

    public static <E, D> PageResult<D> toPageResult(
            Page<E> page,
            Function<E, D> mapper
    ) {
        return new PageResult<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
