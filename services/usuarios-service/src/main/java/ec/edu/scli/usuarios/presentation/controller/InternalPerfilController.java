package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilAuthResponse;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilExistsResponse;
import ec.edu.scli.usuarios.application.usecase.PerfilService;
import ec.edu.scli.usuarios.application.usecase.ContextoInstitucionalService;
import ec.edu.scli.usuarios.domain.model.ContextoInstitucional;
import ec.edu.scli.usuarios.presentation.dto.perfil.ContextoInstitucionalResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Perfiles internos", description = """
                Operaciones internas utilizadas por otros microservicios.
                Este controlador está destinado principalmente a la comunicación
                entre Auth Service y Usuarios Service.
                """)
@RestController
@RequestMapping("/api/v1/internal/perfiles")
public class InternalPerfilController {

        private final PerfilService perfilService;
        private final ContextoInstitucionalService contextoInstitucionalService;
        private final String internalApiKey;

        public InternalPerfilController(
                        PerfilService perfilService,
                        ContextoInstitucionalService contextoInstitucionalService,
                        @Value("${app.internal-api-key}") String internalApiKey) {
                this.perfilService = perfilService;
                this.contextoInstitucionalService = contextoInstitucionalService;
                this.internalApiKey = internalApiKey;
        }

        @Operation(summary = "Verificar la existencia de un perfil", description = """
                        Permite que Auth Service compruebe si un perfil existe,
                        si está activo y qué tipos institucionales tiene asociados.

                        La solicitud debe incluir la cabecera X-Internal-Api-Key.
                        Este endpoint no debe exponerse públicamente sin control
                        del Gateway o de la red interna.
                        """, operationId = "verificarExistenciaPerfil")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "La verificación fue realizada correctamente", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PerfilExistsResponse.class))),
                        @ApiResponse(responseCode = "400", description = "El perfilId no tiene un formato UUID válido", content = @Content),
                        @ApiResponse(responseCode = "401", description = "La clave interna no fue enviada o es incorrecta", headers = @Header(name = "WWW-Authenticate", description = "Indica que la solicitud interna no fue autorizada"), content = @Content),
                        @ApiResponse(responseCode = "500", description = "Ocurrió un error interno en el servidor", content = @Content)
        })
        @GetMapping(value = "/{perfilId}/exists", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<PerfilExistsResponse> verificarExistencia(

                        @Parameter(name = "perfilId", description = "UUID del perfil que se desea validar", required = true, example = "6755fce4-9a44-48c5-9594-228e4667c036") @PathVariable UUID perfilId,

                        @Parameter(name = "X-Internal-Api-Key", description = """
                                        Clave privada utilizada para autorizar la comunicación
                                        entre microservicios.
                                        """, required = true, example = "clave-interna-desarrollo") @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

                if (!claveInternaValida(apiKey)) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .build();
                }

                PerfilExistsResponse response = perfilService.verificarExistencia(perfilId);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Obtener datos de perfil para autenticación", description = """
                        Permite que Auth Service obtenga los datos necesarios
                        del perfil (nombres, apellidos, email institucional,
                        estado y tipos) para construir el JWT durante el login.

                        La solicitud debe incluir la cabecera X-Internal-Api-Key.
                        Este endpoint no debe exponerse públicamente sin control
                        del Gateway o de la red interna.
                        """, operationId = "obtenerPerfilParaAuth")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "El perfil fue encontrado correctamente", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PerfilAuthResponse.class))),
                        @ApiResponse(responseCode = "401", description = "La clave interna no fue enviada o es incorrecta", content = @Content),
                        @ApiResponse(responseCode = "404", description = "No existe un perfil con el id proporcionado", content = @Content)
        })
        @GetMapping(value = "/{perfilId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<PerfilAuthResponse> obtenerParaAuth(

                        @Parameter(name = "perfilId", description = "UUID del perfil solicitado", required = true, example = "6755fce4-9a44-48c5-9594-228e4667c036") @PathVariable UUID perfilId,

                        @Parameter(name = "X-Internal-Api-Key", description = """
                                        Clave privada utilizada para autorizar la comunicación
                                        entre microservicios.
                                        """, required = true, example = "clave-interna-desarrollo") @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

                if (!claveInternaValida(apiKey)) {

                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .build();
                }

                PerfilAuthResponse response = perfilService.obtenerParaAuth(perfilId);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Obtener el contexto institucional de un perfil",
                        description = "Contrato interno de solo lectura para resolver adscripciones organizacionales.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Contexto institucional resuelto",
                                        content = @Content(schema = @Schema(implementation = ContextoInstitucionalResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Clave interna ausente o incorrecta",
                                        content = @Content)
        })
        @GetMapping(value = "/{perfilId}/contexto-institucional", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<ContextoInstitucionalResponse> obtenerContextoInstitucional(
                        @PathVariable UUID perfilId,
                        @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
                if (!claveInternaValida(apiKey)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                return ResponseEntity.ok(toResponse(
                                contextoInstitucionalService.obtenerPorPerfilId(perfilId)));
        }

        private boolean claveInternaValida(String apiKey) {
                return apiKey != null && !apiKey.isBlank() && internalApiKey.equals(apiKey);
        }

        private ContextoInstitucionalResponse toResponse(ContextoInstitucional contexto) {
                ContextoInstitucional.ContextoAdministrador administrador = contexto.administrador();
                return new ContextoInstitucionalResponse(
                                contexto.perfilId(), contexto.existe(), contexto.activo(), contexto.tiposPerfil(),
                                new ContextoInstitucionalResponse.AdministradorInstitucionalResponse(
                                                administrador.esAdministrador(), administrador.activo(),
                                                administrador.pisoId(), administrador.cargo(),
                                                administrador.administradorPisoOperativo()),
                                contexto.adscripciones().stream()
                                                .map(item -> new ContextoInstitucionalResponse.AdscripcionResponse(
                                                                item.tipoAmbito(), item.ambitoId(), item.activo()))
                                                .toList());
        }
}
