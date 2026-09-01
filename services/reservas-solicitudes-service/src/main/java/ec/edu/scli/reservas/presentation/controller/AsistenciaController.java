package ec.edu.scli.reservas.presentation.controller;
import ec.edu.scli.reservas.application.service.AsistenciaService; import ec.edu.scli.reservas.presentation.dto.request.*; import ec.edu.scli.reservas.presentation.dto.response.*; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.security.Principal; import java.util.*; import org.springframework.security.access.AccessDeniedException; import org.springframework.security.core.context.SecurityContextHolder;
@RestController @RequestMapping("/api/v1/asistencias") public class AsistenciaController { private final AsistenciaService service; public AsistenciaController(AsistenciaService s){service=s;}
 @PostMapping("/sesiones") public ResponseEntity<SesionAsistenciaResponse> abrir(@Valid @RequestBody AbrirSesionAsistenciaRequest r,Principal p){return ResponseEntity.status(HttpStatus.CREATED).body(service.abrir(r,id(p)));}
 @GetMapping("/sesiones/{id}") public SesionAsistenciaResponse consultar(@PathVariable UUID id,Principal p){return service.consultar(id,id(p));}
 @PostMapping("/sesiones/{id}/cerrar") @ResponseStatus(HttpStatus.NO_CONTENT) public void cerrar(@PathVariable UUID id,Principal p){service.cerrar(id,id(p));}
 @GetMapping("/sesiones/{id}/registros") public List<RegistroAsistenciaResponse> listar(@PathVariable UUID id,Principal p){return service.listar(id,id(p));}
 @PostMapping("/sesiones/{id}/registros") public ResponseEntity<RegistroAsistenciaResponse> registrar(@PathVariable UUID id,@Valid @RequestBody RegistrarAsistenciaRequest r,Principal p){if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().noneMatch(a -> "ROLE_ESTUDIANTE".equals(a.getAuthority()))) throw new AccessDeniedException("Solo estudiantes pueden registrar asistencia");return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(id,r,id(p)));}
 @GetMapping("/historial") public List<RegistroAsistenciaResponse> historial(Principal p){return service.historial(id(p));}
 private UUID id(Principal p){return UUID.fromString(p.getName());}
}
