package ec.edu.scli.reservas.experimental.port;

import ec.edu.scli.reservas.experimental.domain.ResultadoArbitraje;
import ec.edu.scli.reservas.experimental.domain.SolicitudArbitraje;

public interface ExperimentalAllocationStore {
    ResultadoArbitraje directa(SolicitudArbitraje solicitud, String estrategia);
    ResultadoArbitraje optimista(SolicitudArbitraje solicitud, String estrategia);
    ResultadoArbitraje pesimista(SolicitudArbitraje solicitud, String estrategia);
    ResultadoArbitraje serializable(SolicitudArbitraje solicitud, String estrategia);
}
