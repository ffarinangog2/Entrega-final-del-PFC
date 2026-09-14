package ec.edu.scli.usuarios.presentation.dto.usuarios;

import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UsuarioInstitucionalCreateRequest(
        @NotNull @Valid PerfilCreateRequest perfil,
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String passwordInicial,
        @NotBlank String rol,
        UUID pisoId,
        UUID carreraId,
        UUID periodoId,
        Integer nivel) {
    public UsuarioInstitucionalCreateRequest(PerfilCreateRequest perfil,String username,String email,String passwordInicial,String rol,UUID pisoId,UUID carreraId){this(perfil,username,email,passwordInicial,rol,pisoId,carreraId,null,null);}
}
