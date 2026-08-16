package ec.edu.scli.usuarios.specification;

import ec.edu.scli.usuarios.entity.Perfil;
import org.springframework.data.jpa.domain.Specification;
import ec.edu.scli.usuarios.entity.Administrador;
import ec.edu.scli.usuarios.entity.Docente;
import ec.edu.scli.usuarios.entity.Estudiante;
import ec.edu.scli.usuarios.entity.Tecnico;
import ec.edu.scli.usuarios.enums.TipoPerfil;
import java.util.UUID;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class PerfilSpecification {

    private PerfilSpecification() {
    }

    public static Specification<Perfil> identificacionContiene(
            String identificacion
    ) {
        return (root, query, criteriaBuilder) -> {

            if (identificacion == null || identificacion.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("identificacion")),
                    "%" + identificacion.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Perfil> nombreContiene(
            String nombre
    ) {
        return (root, query, criteriaBuilder) -> {

            if (nombre == null || nombre.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String patron = "%" + nombre.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("nombres")),
                            patron
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("apellidos")),
                            patron
                    )
            );
        };
    }

    public static Specification<Perfil> emailContiene(
            String email
    ) {
        return (root, query, criteriaBuilder) -> {

            if (email == null || email.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String patron = "%" + email.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("emailInstitucional")
                            ),
                            patron
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("emailPersonal")
                            ),
                            patron
                    )
            );
        };
    }

    public static Specification<Perfil> tieneEstado(
            Boolean activo
    ) {
        return (root, query, criteriaBuilder) -> {

            if (activo == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("activo"),
                    activo
            );
        };
    }
    public static Specification<Perfil> tieneTipoPerfil(
            TipoPerfil tipoPerfil
    ) {

        return (root, query, criteriaBuilder) -> {

            if (tipoPerfil == null) {
                return criteriaBuilder.conjunction();
            }

            return switch (tipoPerfil) {

                case DOCENTE -> {

                    Subquery<UUID> subquery =
                            query.subquery(UUID.class);

                    Root<Docente> docente =
                            subquery.from(Docente.class);

                    subquery.select(
                            docente.get("perfil").get("id")
                    );

                    subquery.where(
                            criteriaBuilder.equal(
                                    docente.get("perfil").get("id"),
                                    root.get("id")
                            )
                    );

                    yield criteriaBuilder.exists(subquery);
                }

                case ESTUDIANTE -> {

                    Subquery<UUID> subquery =
                            query.subquery(UUID.class);

                    Root<Estudiante> estudiante =
                            subquery.from(Estudiante.class);

                    subquery.select(
                            estudiante.get("perfil").get("id")
                    );

                    subquery.where(
                            criteriaBuilder.equal(
                                    estudiante.get("perfil").get("id"),
                                    root.get("id")
                            )
                    );

                    yield criteriaBuilder.exists(subquery);
                }

                case TECNICO -> {

                    Subquery<UUID> subquery =
                            query.subquery(UUID.class);

                    Root<Tecnico> tecnico =
                            subquery.from(Tecnico.class);

                    subquery.select(
                            tecnico.get("perfil").get("id")
                    );

                    subquery.where(
                            criteriaBuilder.equal(
                                    tecnico.get("perfil").get("id"),
                                    root.get("id")
                            )
                    );

                    yield criteriaBuilder.exists(subquery);
                }

                case ADMINISTRADOR -> {

                    Subquery<UUID> subquery =
                            query.subquery(UUID.class);

                    Root<Administrador> administrador =
                            subquery.from(Administrador.class);

                    subquery.select(
                            administrador.get("perfil").get("id")
                    );

                    subquery.where(
                            criteriaBuilder.equal(
                                    administrador
                                            .get("perfil")
                                            .get("id"),
                                    root.get("id")
                            )
                    );

                    yield criteriaBuilder.exists(subquery);
                }
            };
        };
    }
}