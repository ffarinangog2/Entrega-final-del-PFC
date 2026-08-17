package ec.edu.scli.usuarios.infrastructure.observer;

import ec.edu.scli.usuarios.domain.event.PerfilEvent;
import ec.edu.scli.usuarios.domain.event.PerfilEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPerfilEventListener implements PerfilEventListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LoggingPerfilEventListener.class);

    @Override
    public void onPerfilEvent(PerfilEvent event) {
        LOGGER.info(
                "perfil_event tipo={} perfilId={} activo={} ocurridoEn={}",
                event.tipo(),
                event.perfilId(),
                event.activo(),
                event.ocurridoEn()
        );
    }
}
