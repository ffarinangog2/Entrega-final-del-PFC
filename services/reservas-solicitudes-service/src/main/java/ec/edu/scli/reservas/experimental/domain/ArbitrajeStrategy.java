package ec.edu.scli.reservas.experimental.domain;

public interface ArbitrajeStrategy {
    String nombre();
    ResultadoArbitraje adjudicar(SolicitudArbitraje solicitud);
}
