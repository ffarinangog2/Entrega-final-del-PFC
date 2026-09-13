package ec.edu.uteq.scli.auth_service.domain.service;

public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException() {
        super("La cuenta se encuentra bloqueada");
    }
}
