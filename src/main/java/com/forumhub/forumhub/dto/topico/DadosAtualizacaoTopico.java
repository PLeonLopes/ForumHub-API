package com.forumhub.forumhub.dto.topico;

import com.forumhub.forumhub.model.StatusTopico;

public record DadosAtualizacaoTopico(String titulo, String mensagem, StatusTopico status) {
}