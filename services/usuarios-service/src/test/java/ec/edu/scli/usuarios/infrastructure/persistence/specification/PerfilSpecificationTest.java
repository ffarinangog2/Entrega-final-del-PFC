package ec.edu.scli.usuarios.infrastructure.persistence.specification;

import ec.edu.scli.usuarios.domain.model.TipoPerfil;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class PerfilSpecificationTest {

    private Root<Perfil> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;
    private Path path;
    private Predicate conjuncion;
    private Predicate predicado;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        path = mock(Path.class);
        conjuncion = mock(Predicate.class);
        predicado = mock(Predicate.class);
    }

    // ---------------------------------------------------------------
    // identificacionHashIgual
    // ---------------------------------------------------------------

    @Test
    void identificacionHashIgual_conValorNulo_retornaConjuncion() {
        when(cb.conjunction()).thenReturn(conjuncion);

        Predicate resultado = PerfilSpecification
                .identificacionHashIgual(null)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(conjuncion);
    }

    @Test
    void identificacionHashIgual_conValorEnBlanco_retornaConjuncion() {
        when(cb.conjunction()).thenReturn(conjuncion);

        Predicate resultado = PerfilSpecification
                .identificacionHashIgual("   ")
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(conjuncion);
    }

    @Test
    void identificacionHashIgual_conValor_retornaEqual() {
        when(root.get("identificacionHash")).thenReturn(path);
        when(cb.equal(eq(path), eq("abc123hash"))).thenReturn(predicado);

        Predicate resultado = PerfilSpecification
                .identificacionHashIgual("abc123hash")
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(predicado);
        verify(cb).equal(path, "abc123hash");
    }

    // ---------------------------------------------------------------
    // nombreContiene
    // ---------------------------------------------------------------

    @Test
    void nombreContiene_conValorNulo_retornaConjuncion() {
        when(cb.conjunction()).thenReturn(conjuncion);

        Predicate resultado = PerfilSpecification
                .nombreContiene(null)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(conjuncion);
    }

    @Test
    void nombreContiene_conValor_retornaOrDeLikes() {
        when(root.get(anyString())).thenReturn(path);
        when(cb.lower(any())).thenReturn(path);
        when(cb.like(any(), anyString())).thenReturn(predicado);
        when(cb.or(any(), any())).thenReturn(predicado);

        Predicate resultado = PerfilSpecification
                .nombreContiene("Ana")
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(predicado);
        verify(cb).or(predicado, predicado);
    }

    // ---------------------------------------------------------------
    // emailInstitucionalContiene
    // ---------------------------------------------------------------

    @Test
    void emailInstitucionalContiene_conValorNulo_retornaConjuncion() {
        when(cb.conjunction()).thenReturn(conjuncion);

        Predicate resultado = PerfilSpecification
                .emailInstitucionalContiene(null)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(conjuncion);
    }

    @Test
    void emailInstitucionalContiene_conValor_retornaLike() {
        when(root.get("emailInstitucional")).thenReturn(path);
        when(cb.lower(any())).thenReturn(path);
        when(cb.like(any(), anyString())).thenReturn(predicado);

        Predicate resultado = PerfilSpecification
                .emailInstitucionalContiene("uteq.edu.ec")
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(predicado);
        verify(cb).like(eq(path), eq("%uteq.edu.ec%"));
    }

    // ---------------------------------------------------------------
    // tieneEstado
    // ---------------------------------------------------------------

    @Test
    void tieneEstado_conValorNulo_retornaConjuncion() {
        when(cb.conjunction()).thenReturn(conjuncion);

        Predicate resultado = PerfilSpecification
                .tieneEstado(null)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(conjuncion);
    }

    @Test
    void tieneEstado_conValor_retornaEqual() {
        when(root.get("activo")).thenReturn(path);
        when(cb.equal(eq(path), eq(true))).thenReturn(predicado);

        Predicate resultado = PerfilSpecification
                .tieneEstado(true)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(predicado);
    }

    // ---------------------------------------------------------------
    // tieneTipoPerfil
    // ---------------------------------------------------------------

    @Test
    void tieneTipoPerfil_conValorNulo_retornaConjuncion() {
        when(cb.conjunction()).thenReturn(conjuncion);

        Predicate resultado = PerfilSpecification
                .tieneTipoPerfil(null)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(conjuncion);
    }

    private void stubSubquery() {
        Subquery subquery = mock(Subquery.class);
        Root subRoot = mock(Root.class);
        Path subPath = mock(Path.class);

        when(query.subquery(eq(java.util.UUID.class))).thenReturn(subquery);
        when(subquery.from(any(Class.class))).thenReturn(subRoot);
        when(subRoot.get(anyString())).thenReturn(subPath);
        when(subPath.get(anyString())).thenReturn(subPath);
        when(root.get(anyString())).thenReturn(path);
        when(subquery.select(any())).thenReturn(subquery);
        when(cb.equal(any(), any())).thenReturn(predicado);
        when(subquery.where(any(Predicate.class))).thenReturn(subquery);
        when(cb.exists(any())).thenReturn(predicado);
    }

    @Test
    void tieneTipoPerfil_docente_retornaExists() {
        stubSubquery();

        Predicate resultado = PerfilSpecification
                .tieneTipoPerfil(TipoPerfil.DOCENTE)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(predicado);
    }

    @Test
    void tieneTipoPerfil_estudiante_retornaExists() {
        stubSubquery();

        Predicate resultado = PerfilSpecification
                .tieneTipoPerfil(TipoPerfil.ESTUDIANTE)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(predicado);
    }

    @Test
    void tieneTipoPerfil_administrador_retornaExists() {
        stubSubquery();

        Predicate resultado = PerfilSpecification
                .tieneTipoPerfil(TipoPerfil.ADMINISTRADOR)
                .toPredicate(root, query, cb);

        assertThat(resultado).isEqualTo(predicado);
    }
}
