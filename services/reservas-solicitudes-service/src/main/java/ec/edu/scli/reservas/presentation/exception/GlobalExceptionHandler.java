package ec.edu.scli.reservas.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;

/** Centraliza el manejo de excepciones producidas por la API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> manejarIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> manejarNoEncontrado(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> manejarIllegalState(
            IllegalStateException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacionArgumentos(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                primerMensajeValidacion(exception),
                request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> manejarViolacionRestriccion(
            ConstraintViolationException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> manejarParametrosInvalidos(
            Exception exception, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "La solicitud contiene parámetros inválidos", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> manejarAccesoDenegado(
            AccessDeniedException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> manejarBloqueoOptimista(
            ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "La operación entra en conflicto con el estado actual", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> manejarIntegridadDatos(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "La operación viola una restricción de integridad", request);
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public ResponseEntity<ApiError> manejarAccesoProhibido(
            HttpClientErrorException.Forbidden exception, HttpServletRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> manejarRecursoNoDisponible(
            ResourceAccessException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "Un servicio requerido no está disponible", request);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ApiError> manejarErrorServicioExterno(
            HttpServerErrorException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "Un servicio requerido no está disponible", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarExcepcion(
            Exception exception, HttpServletRequest request) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno", request);
    }

    private String primerMensajeValidacion(BindException exception) {
        if (!exception.getBindingResult().getAllErrors().isEmpty()) {
            String mensaje = exception.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
            if (mensaje != null && !mensaje.isBlank()) {
                return mensaje;
            }
        }
        return "La solicitud contiene datos inválidos";
    }

    private ResponseEntity<ApiError> respuesta(
            HttpStatus status, String message, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
