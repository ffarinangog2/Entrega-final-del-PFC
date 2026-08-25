package ec.edu.scli.reservas.domain.port.out;

import ec.edu.scli.reservas.domain.model.ActorAutenticado;

public interface ActorActualPort {
    ActorAutenticado obtener();
}
