package ec.edu.scli.reservas.domain.model;

public enum EstadoIncidente {
    REPORTADO, EN_REVISION, RESUELTO;

    public boolean puedeTransicionarA(EstadoIncidente destino) {
        return (this == REPORTADO && destino == EN_REVISION)
                || (this == EN_REVISION && destino == RESUELTO);
    }
}
