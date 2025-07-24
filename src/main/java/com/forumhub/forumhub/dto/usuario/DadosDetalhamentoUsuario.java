package com.forumhub.forumhub.dto.usuario;

import com.forumhub.forumhub.model.Usuario;

public record DadosDetalhamentoUsuario(Long id, String login) {

    public DadosDetalhamentoUsuario(Usuario usuario) {
        this(usuario.getId(), usuario.getLogin());
    }
}
