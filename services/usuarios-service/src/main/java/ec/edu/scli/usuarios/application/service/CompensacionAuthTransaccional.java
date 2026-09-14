package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.infrastructure.client.AuthAdminClient;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AuthUsuarioResponse;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AuthUsuarioUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CompensacionAuthTransaccional {
    private static final Logger log = LoggerFactory.getLogger(CompensacionAuthTransaccional.class);
    private final AuthAdminClient auth;

    public CompensacionAuthTransaccional(AuthAdminClient auth) {
        this.auth = auth;
    }

    public void registrarCreacion(AuthUsuarioResponse creado) {
        registrar(() -> auth.eliminarCredencialCreada(creado.id(), creado.perfilId()));
    }

    public void registrarRestauracion(AuthUsuarioResponse anterior) {
        registrar(() -> auth.actualizar(anterior.id(), new AuthUsuarioUpdateRequest(
                anterior.username(), anterior.email(), anterior.rol(), anterior.activo())));
    }

    private void registrar(Runnable compensacion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            compensar(compensacion);
            throw new IllegalStateException("No existe una transacción local para coordinar la operación de Auth.");
        }
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) compensar(compensacion);
                }
            });
        } catch (RuntimeException error) {
            compensar(compensacion);
            throw error;
        }
    }

    private void compensar(Runnable compensacion) {
        RuntimeException ultimoError = null;
        for (int intento = 1; intento <= 3; intento++) {
            try {
                compensacion.run();
                return;
            } catch (RuntimeException error) {
                ultimoError = error;
                log.warn("Falló el intento {} de compensación administrativa en Auth", intento, error);
            }
        }
        log.error("No fue posible completar la compensación administrativa en Auth", ultimoError);
    }
}
