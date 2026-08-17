package ec.edu.scli.usuarios.presentation.exception;

import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ConflictException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private MethodParameter methodParameter;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/estudiantes/123");
    }

    @Test
    void manejarRecursoNoEncontrado_deberiaRetornar404() {
        ResponseEntity<ApiError> respuesta = handler.manejarRecursoNoEncontrado(
                new ResourceNotFoundException("no existe"), request
        );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().message()).isEqualTo("no existe");
        assertThat(respuesta.getBody().path()).isEqualTo("/api/v1/estudiantes/123");
    }

    @Test
    void manejarConflicto_deberiaRetornar409() {
        ResponseEntity<ApiError> respuesta = handler.manejarConflicto(
                new ConflictException("duplicado"), request
        );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().message()).isEqualTo("duplicado");
    }

    @Test
    void manejarReglaNegocio_deberiaRetornar422() {
        ResponseEntity<ApiError> respuesta = handler.manejarReglaNegocio(
                new BusinessRuleException("regla violada"), request
        );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody().message()).isEqualTo("regla violada");
    }

    @Test
    void manejarRutaNoEncontrada_deberiaRetornar404() {
        NoResourceFoundException exception =
                new NoResourceFoundException(HttpMethod.GET, "/no/existe");

        ResponseEntity<ApiError> respuesta =
                handler.manejarRutaNoEncontrada(exception, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().message()).isEqualTo("La ruta solicitada no existe");
    }

    @Test
    void manejarValidaciones_deberiaRetornar400ConErroresPorCampo() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "estudianteRequest");
        bindingResult.addError(new FieldError(
                "estudianteRequest", "matricula", "La matrícula es obligatoria"
        ));

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ApiError> respuesta =
                handler.manejarValidaciones(exception, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().message()).isEqualTo("Los datos enviados no son válidos");
        assertThat(respuesta.getBody().validationErrors())
                .containsEntry("matricula", "La matrícula es obligatoria");
    }

    @Test
    void manejarErrorGeneral_deberiaRetornar500() {
        ResponseEntity<ApiError> respuesta = handler.manejarErrorGeneral(
                new RuntimeException("boom"), request
        );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().message())
                .isEqualTo("Ha ocurrido un error interno en el servidor");
    }
}
