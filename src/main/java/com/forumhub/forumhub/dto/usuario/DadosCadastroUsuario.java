package com.forumhub.forumhub.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DadosCadastroUsuario(
        @NotBlank
        @Email
        String login,

        @NotBlank
        @Size(min = 6)
        String senha) {
}
