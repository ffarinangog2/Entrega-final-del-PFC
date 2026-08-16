package ec.edu.uteq.scli.auth_service.exception;

import ec.edu.uteq.scli.auth_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(
                        InvalidCredentialsException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                exception.getMessage(),
                                request.getRequestURI());
        }

        @ExceptionHandler(AccountBlockedException.class)
        public ResponseEntity<ErrorResponse> handleAccountBlocked(
                        AccountBlockedException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.LOCKED,
                                exception.getMessage(),
                                request.getRequestURI());
        }

        @ExceptionHandler(AccountDisabledException.class)
        public ResponseEntity<ErrorResponse> handleAccountDisabled(
                        AccountDisabledException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.FORBIDDEN,
                                exception.getMessage(),
                                request.getRequestURI());
        }

        @ExceptionHandler(UsuarioServiceUnavailableException.class)
        public ResponseEntity<ErrorResponse> handleUsuarioServiceUnavailable(
                        UsuarioServiceUnavailableException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "El servicio de usuarios no está disponible. Intenta nuevamente en unos momentos.",
                                request.getRequestURI());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException exception,
                        HttpServletRequest request) {
                String message = exception
                                .getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " +
                                                error.getDefaultMessage())
                                .collect(Collectors.joining("; "));

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                message,
                                request.getRequestURI());
        }

        private ResponseEntity<ErrorResponse> buildResponse(
                        HttpStatus status,
                        String message,
                        String path) {
                ErrorResponse body = new ErrorResponse(
                                OffsetDateTime.now(ZoneOffset.UTC),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                path);

                return ResponseEntity
                                .status(status)
                                .body(body);
        }

}