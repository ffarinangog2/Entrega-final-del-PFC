package ec.edu.scli.reservas.domain;
import ec.edu.scli.reservas.domain.model.EstadoIncidente;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EstadoIncidenteTest {
 @Test void permiteSoloFlujoDefinido(){
  assertTrue(EstadoIncidente.REPORTADO.puedeTransicionarA(EstadoIncidente.EN_REVISION));
  assertTrue(EstadoIncidente.EN_REVISION.puedeTransicionarA(EstadoIncidente.RESUELTO));
  assertFalse(EstadoIncidente.REPORTADO.puedeTransicionarA(EstadoIncidente.RESUELTO));
  assertFalse(EstadoIncidente.RESUELTO.puedeTransicionarA(EstadoIncidente.EN_REVISION));
 }
}
