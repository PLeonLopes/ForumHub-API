package com.forumhub.forumhub.dto.resposta;

import jakarta.validation.constraints.NotBlank;

public record DadosCadastroResposta(@NotBlank String mensagem) {
}