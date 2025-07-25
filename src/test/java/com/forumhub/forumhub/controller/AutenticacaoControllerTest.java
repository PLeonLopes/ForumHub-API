package com.forumhub.forumhub.controller;

import com.forumhub.forumhub.dto.auth.DadosAutenticacao;
import com.forumhub.forumhub.model.Usuario;
import com.forumhub.forumhub.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@ActiveProfiles("test")
class AutenticacaoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JacksonTester<DadosAutenticacao> dadosAutenticacaoJson;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        usuarioRepository.deleteAll();

        var senhacript = passwordEncoder.encode("123456");
        var usuario = new Usuario(null, "ana.souza@voll.med", senhacript);

        usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("Deve retornar http 403 (Forbidden) para credenciais inválidas")
    void efetuarLogin_cenario1() throws Exception {
        var dadosAutenticacao = new DadosAutenticacao("usuario.teste@email.com", "senhaTeste");

        mvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosAutenticacaoJson.write(dadosAutenticacao).getJson())
        ).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar http 200 (OK) e token JWT para credênciais válidas")
    void efetuarLogin_cenario2() throws Exception {
        var dadosAutenticacao = new DadosAutenticacao("ana.souza@voll.med", "123456");

        mvc.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(dadosAutenticacaoJson.write(dadosAutenticacao).getJson())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
