package ec.edu.scli.usuarios.exception;

public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String mensaje) {
        super(mensaje);
    }
}