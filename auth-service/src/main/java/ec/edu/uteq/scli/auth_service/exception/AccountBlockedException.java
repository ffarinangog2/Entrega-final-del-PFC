package ec.edu.uteq.scli.auth_service.exception;

public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException() {
        super("La cuenta se encuentra bloqueada");
    }
}