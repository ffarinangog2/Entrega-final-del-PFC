package ec.edu.scli.usuarios.presentation.dto.usuarios;

import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UsuarioInstitucionalUpdateRequest(
        @NotNull UUID authId,
        @NotNull @Valid PerfilUpdateRequest perfil,
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String rol,
        boolean activo,
        UUID pisoId,
        UUID carreraId,
        UUID periodoId,
        Integer nivel) {
    public UsuarioInstitucionalUpdateRequest(UUID authId,PerfilUpdateRequest perfil,String username,String email,String rol,boolean activo,UUID pisoId,UUID carreraId){this(authId,perfil,username,email,rol,activo,pisoId,carreraId,null,null);}
}
