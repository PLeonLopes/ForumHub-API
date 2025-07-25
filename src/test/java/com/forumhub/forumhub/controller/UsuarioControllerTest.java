package com.forumhub.forumhub.controller;

import com.forumhub.forumhub.dto.usuario.DadosCadastroUsuario;
import com.forumhub.forumhub.model.Usuario;
import com.forumhub.forumhub.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@ActiveProfiles("test")
public class UsuarioControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JacksonTester<DadosCadastroUsuario> dadosCadastroJson;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST(/usuarios) - Deve retornar http 201 (Created) ao cadastrar novo usuário")
    @Transactional
    void cadastrar_cenario1() throws Exception {
        var dadosCadastro = new DadosCadastroUsuario("novo.user@email.com", "senha123");

        mvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosCadastroJson.write(dadosCadastro).getJson()))
                .andExpect(status().isCreated());

        var usuarioSalvo = usuarioRepository.findByLogin("novo.user@email.com").orElseThrow();
        assertThat(usuarioSalvo).isNotNull();
        assertThat(passwordEncoder.matches("senha123", usuarioSalvo.getPassword())).isTrue();
    }

    @Test
    @DisplayName("POST(/usuarios) - Deve retornar http 400 (Bad request) ao tentar cadastrar login duplicado")
    @Transactional
    void cadastrar_cenario2() throws Exception {
        // ARRANGE
        var usuarioExistente = new Usuario();
        usuarioExistente.setLogin("usuario.exist@email.com");
        usuarioExistente.setSenha(passwordEncoder.encode("senha123"));
        usuarioRepository.saveAndFlush(usuarioExistente);

        var dadosCadastroDuplicado = new DadosCadastroUsuario("usuario.exist@email.com", "senha321");

        // ACT & ASSERT
        mvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosCadastroJson.write(dadosCadastroDuplicado).getJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("GET(/usuarios/me) - Deve retornar http 403 (Forbidden) se o usuário não estiver autenticado ainda ")
    void detalharUsuarioLogado_cenario1() throws Exception {
        // ACT & ASSERT
        mvc.perform(get("/usuarios/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET(/usuarios/me) - Deve retornar http 200 (OK) + dados do usuário autenticado")
    @WithMockUser(username = "usuario.logado@email.com")
    @Transactional
    void detalharUsuarioLogado_cenario2() throws Exception {
        // ARRANGE
        var usuario = new Usuario();
        usuario.setLogin("usuario.log@email.com");
        usuario.setSenha(passwordEncoder.encode("senha123"));
        usuarioRepository.saveAndFlush(usuario);

        // ACT & ASSERT
        mvc.perform(get("/usuarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("usuario.log@email.com"));
    }
}
