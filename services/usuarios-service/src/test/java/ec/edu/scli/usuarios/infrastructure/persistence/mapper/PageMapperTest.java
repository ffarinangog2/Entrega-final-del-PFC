package ec.edu.scli.usuarios.infrastructure.persistence.mapper;

import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.pagination.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageMapperTest {

    @Test
    void toPageable_conCriteriaNula_retornaUnpaged() {
        Pageable pageable = PageMapper.toPageable(null);

        assertThat(pageable.isUnpaged()).isTrue();
    }

    @Test
    void toPageable_sinOrden_retornaPageableSinSort() {
        Pageable pageable = PageMapper.toPageable(new PageCriteria(1, 5, List.of()));

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void toPageable_conOrdenAscYDesc_mapeaDirecciones() {
        PageCriteria criteria = new PageCriteria(0, 10, List.of(
                new SortOrder("nombres", SortOrder.Direction.ASC),
                new SortOrder("id", SortOrder.Direction.DESC)
        ));

        Pageable pageable = PageMapper.toPageable(criteria);

        assertThat(pageable.getSort().getOrderFor("nombres").isAscending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void toPageResult_deberiaMapearPageAPageResult() {
        Page<Integer> page = new PageImpl<>(
                List.of(1, 2, 3), PageRequest.of(0, 10), 3
        );

        PageResult<String> resultado = PageMapper.toPageResult(page, i -> "n" + i);

        assertThat(resultado.content()).containsExactly("n1", "n2", "n3");
        assertThat(resultado.totalElements()).isEqualTo(3);
        assertThat(resultado.page()).isZero();
        assertThat(resultado.size()).isEqualTo(10);
    }
}
