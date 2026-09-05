package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.pagination.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageableMapperTest {

    @Test
    void toCriteria_conPageableNulo_retornaCriteriaPorDefecto() {
        PageCriteria criteria = PageableMapper.toCriteria(null);

        assertThat(criteria.page()).isZero();
        assertThat(criteria.size()).isEqualTo(Integer.MAX_VALUE);
        assertThat(criteria.sort()).isEmpty();
    }

    @Test
    void toCriteria_conPageableNoPaginado_retornaCriteriaPorDefecto() {
        PageCriteria criteria = PageableMapper.toCriteria(Pageable.unpaged());

        assertThat(criteria.page()).isZero();
        assertThat(criteria.size()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void toCriteria_conOrdenAscendenteYDescendente_mapeaDireccion() {
        Pageable pageable = PageRequest.of(
                1, 5,
                Sort.by(Sort.Order.asc("nombres"), Sort.Order.desc("id"))
        );

        PageCriteria criteria = PageableMapper.toCriteria(pageable);

        assertThat(criteria.page()).isEqualTo(1);
        assertThat(criteria.size()).isEqualTo(5);
        assertThat(criteria.sort()).containsExactly(
                new SortOrder("nombres", SortOrder.Direction.ASC),
                new SortOrder("id", SortOrder.Direction.DESC)
        );
    }

    @Test
    void toSpringPage_deberiaMapearPageResultAPageImpl() {
        PageResult<String> pageResult = new PageResult<>(
                List.of("a", "b"), 2, 1, 0, 10
        );

        PageImpl<String> springPage = PageableMapper.toSpringPage(pageResult);

        assertThat(springPage.getContent()).containsExactly("a", "b");
        assertThat(springPage.getTotalElements()).isEqualTo(2);
        assertThat(springPage.getNumber()).isZero();
        assertThat(springPage.getSize()).isEqualTo(10);
    }
}
