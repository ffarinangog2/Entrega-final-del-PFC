package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.infrastructure.client.AuthAdminClient;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AuthUsuarioResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompensacionAuthTransaccionalTest {
    @Mock AuthAdminClient auth;
    private CompensacionAuthTransaccional compensaciones;

    @BeforeEach
    void setUp() {
        compensaciones = new CompensacionAuthTransaccional(auth);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void authOkYRollbackLocalEliminaLaCredencialRecienCreada() {
        UUID authId = UUID.randomUUID();
        UUID perfilId = UUID.randomUUID();
        compensaciones.registrarCreacion(new AuthUsuarioResponse(
                authId, perfilId, "admin.piso", "admin@scli.edu.ec", "ADMINISTRADOR_PISO", true));

        completar(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(auth).eliminarCredencialCreada(authId, perfilId);
    }

    @Test
    void rollbackLocalRestauraRolYEstadoAnteriores() {
        UUID authId = UUID.randomUUID();
        var anterior = new AuthUsuarioResponse(
                authId, UUID.randomUUID(), "usuario", "usuario@scli.edu.ec", "DOCENTE", false);
        compensaciones.registrarRestauracion(anterior);

        completar(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(auth).actualizar(org.mockito.ArgumentMatchers.eq(authId), argThat(request ->
                request.rol().equals("DOCENTE") && !request.activo()));
    }

    @Test
    void commitConfirmadoNoEjecutaCompensacion() {
        UUID authId = UUID.randomUUID();
        UUID perfilId = UUID.randomUUID();
        compensaciones.registrarCreacion(new AuthUsuarioResponse(
                authId, perfilId, "admin", "admin@scli.edu.ec", "ADMINISTRADOR", true));

        completar(TransactionSynchronization.STATUS_COMMITTED);

        verify(auth, never()).eliminarCredencialCreada(authId, perfilId);
    }

    private void completar(int estado) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(item -> item.afterCompletion(estado));
    }
}
