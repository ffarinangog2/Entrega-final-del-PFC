package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.SortOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

final class PageableMapper {

    private PageableMapper() {
    }

    static PageCriteria toCriteria(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return new PageCriteria(0, Integer.MAX_VALUE, List.of());
        }

        return new PageCriteria(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().stream()
                        .map(order -> new SortOrder(
                                order.getProperty(),
                                order.isDescending()
                                        ? SortOrder.Direction.DESC
                                        : SortOrder.Direction.ASC
                        ))
                        .toList()
        );
    }

    static <T> PageImpl<T> toSpringPage(
            ec.edu.scli.usuarios.domain.pagination.PageResult<T> page
    ) {
        Pageable pageable = PageRequest.of(
                page.page(),
                page.size(),
                Sort.unsorted()
        );
        return new PageImpl<>(page.content(), pageable, page.totalElements());
    }
}
