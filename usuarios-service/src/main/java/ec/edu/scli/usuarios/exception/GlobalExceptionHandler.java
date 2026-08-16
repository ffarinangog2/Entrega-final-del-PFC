package ec.edu.scli.usuarios.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> manejarRecursoNoEncontrado(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                OffsetDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }


    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> manejarConflicto(
            ConflictException exception,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                OffsetDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }


    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> manejarReglaNegocio(
            BusinessRuleException exception,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                OffsetDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error);
    }
//nuevo
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> manejarRutaNoEncontrada(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                OffsetDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "La ruta solicitada no existe",
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidaciones(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> errores = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {

            errores.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        ApiError error = new ApiError(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Los datos enviados no son válidos",
                request.getRequestURI(),
                errores
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarErrorGeneral(
            Exception exception,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                OffsetDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ha ocurrido un error interno en el servidor",
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}