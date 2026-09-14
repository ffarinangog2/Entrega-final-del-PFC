package ec.edu.uteq.scli.auth_service.domain.service;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("La cuenta se encuentra desactivada");
    }
}
