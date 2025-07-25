package com.forumhub.forumhub.controller;

import com.forumhub.forumhub.dto.resposta.DadosAtualizacaoResposta;
import com.forumhub.forumhub.dto.resposta.DadosCadastroResposta;
import com.forumhub.forumhub.model.Resposta;
import com.forumhub.forumhub.model.StatusTopico;
import com.forumhub.forumhub.model.Topico;
import com.forumhub.forumhub.model.Usuario;
import com.forumhub.forumhub.repository.RespostaRepository;
import com.forumhub.forumhub.repository.TopicoRepository;
import com.forumhub.forumhub.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.hamcrest.Matchers.hasSize;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@ActiveProfiles("test")
class RespostaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JacksonTester<DadosCadastroResposta> dadosCadastroRespostaJson;

    @Autowired
    private JacksonTester<DadosAtualizacaoResposta> dadosAtualizacaoRespostaJson;

    @Autowired
    private TopicoRepository topicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RespostaRepository respostaRepository;

    private Usuario autor;
    private Topico topico;

    @BeforeEach
    void setUp() {
        respostaRepository.deleteAll();
        topicoRepository.deleteAll();
        usuarioRepository.deleteAll();

        this.autor = usuarioRepository.save(new Usuario(null, "autor.teste", "123123"));

        this.topico = new Topico(null, "Tópico Teste", "Mensagem do tópico", true, LocalDateTime.now(), StatusTopico.NAO_RESPONDIDO, null, autor.getUsername(), "Spring Boot");
        topicoRepository.save(this.topico);
    }

    @Test
    @DisplayName("Deve retornar http 403 (Forbidden) se não estiver autenticado")
    void criarResposta_cenario1() throws Exception {
        var dados = new DadosCadastroResposta("Resposta - teste.");

        mvc.perform(post("/topicos/" + topico.getId() + "/respostas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosCadastroRespostaJson.write(dados).getJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar http 404 (Not found) se tópico inexistente")
    void criarResposta_cenario2() throws Exception {
        var dados = new DadosCadastroResposta("Resposta para tópico que não existe.");
        long idTopicoInexistente = 999L;

        mvc.perform(post("/topicos/" + idTopicoInexistente + "/respostas")
                        .with(user(this.autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosCadastroRespostaJson.write(dados).getJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar http 201 (Created) se resposta válida ")
    void criarResposta_cenario3() throws Exception {
        var dados = new DadosCadastroResposta("Resposta teste criada com sucesso!");
        var autorResposta = usuarioRepository.save(new Usuario(null, "outro.autor", "123"));

        var response = mvc.perform(post("/topicos/" + topico.getId() + "/respostas")
                        .with(user(autorResposta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosCadastroRespostaJson.write(dados).getJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        var jsonRetornado = response.getContentAsString();

        String mensagemDoJson = JsonPath.read(jsonRetornado, "$.mensagem");
        String autorDoJson = JsonPath.read(jsonRetornado, "$.nomeAutor"); // <<-- CORREÇÃO AQUI

        assertThat(mensagemDoJson).isEqualTo(dados.mensagem());
        assertThat(autorDoJson).isEqualTo(autorResposta.getUsername());
    }

    @Test
    @DisplayName("GET - Deve retornar http 200 (OK) e listar respostas de um tópico")
    void listarRespostas_cenario1() throws Exception {

        // Arrange
        var autorResposta = usuarioRepository.save(new Usuario(null, "comentarista", "123"));
        respostaRepository.save(new Resposta(null, "Primeira resposta", this.topico, LocalDateTime.now(), autorResposta, false));
        respostaRepository.save(new Resposta(null, "Segunda resposta", this.topico, LocalDateTime.now(), autorResposta, false));

        // Act & Assert
        mvc.perform(get("/topicos/" + this.topico.getId() + "/respostas")
                        .with(user(this.autor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET - Deve retornar http 200 (OK)")
    void listarRespostas_cenario2() throws Exception {

        // Act & Assert
        mvc.perform(get("/topicos/" + this.topico.getId() + "/respostas")
                        .with(user(this.autor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("PUT - Deve retornar http 200 (OK) se editar a propria resposta")
    void atualizarResposta_cenario1() throws Exception {
        // Arrange
        var autorResposta = usuarioRepository.save(new Usuario(null, "editor", "123"));
        var resposta = respostaRepository.save(new Resposta(null, "Msg original", this.topico, LocalDateTime.now(), autorResposta, false));
        var dadosAtualizacao = new DadosAtualizacaoResposta("Msg atualizada!");

        // Act & Assert
        mvc.perform(put("/topicos/{idTopico}/respostas/{idResposta}", this.topico.getId(), resposta.getId())
                        .with(user(autorResposta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosAtualizacaoRespostaJson.write(dadosAtualizacao).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Msg atualizada!"));
    }

    @Test
    @DisplayName("PUT - Deve retornar http 400 (Bad Request) se editar resposta de outro usuário")
    void atualizarResposta_cenario2() throws Exception {
        // Arrange
        var autorResposta = usuarioRepository.save(new Usuario(null, "dono.resposta", "123"));
        var outroUsuario = usuarioRepository.save(new Usuario(null, "outroUsuario", "123"));
        var resposta = respostaRepository.save(new Resposta(null, "Msg original", this.topico, LocalDateTime.now(), autorResposta, false));
        var dadosAtualizacao = new DadosAtualizacaoResposta("Tentatia de Edição Inválida!");

        // Act & Assert
        mvc.perform(put("/topicos/{idTopico}/respostas/{idResposta}", this.topico.getId(), resposta.getId())
                        .with(user(outroUsuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosAtualizacaoRespostaJson.write(dadosAtualizacao).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE - Deve retornar http 204 (No Content) se autor excluir propria resposta")
    void deletarResposta_cenario1() throws Exception {
        var autorResposta = usuarioRepository.save(new Usuario(null, "deletador", "123"));

        var resposta = respostaRepository.save(new Resposta(null, "Resposta escolhida", topico, LocalDateTime.now(), autorResposta, false));

        mvc.perform(
                delete("/topicos/{idTopico}/respostas/{idResposta}", topico.getId(), resposta.getId())
                        .with(user(autorResposta))
        ).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE - Deve retornar http 400 (Bad Request) se usuário tentar excluir resposta de outro")
    void deletarResposta_cenario2() throws Exception {
        var autorResposta = usuarioRepository.save(new Usuario(null, "dono.resposta", "123"));
        var outroUsuario = usuarioRepository.save(new Usuario(null, "outro.usuario", "123"));

        var resposta = new Resposta(null, "Resposta de outro usuário.", topico, LocalDateTime.now(), autorResposta, false);
        respostaRepository.save(resposta);

        mvc.perform(
                delete("/topicos/{idTopico}/respostas/{idResposta}", topico.getId(), resposta.getId())
                        .with(user(outroUsuario))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE - Deve retornar 204 (No content) se autor do tópico exclui uma resposta")
    void deletarResposta_cenario3() throws Exception {
        // Arrange
        var autorResposta = usuarioRepository.save(new Usuario(null, "autor.resposta", "123"));
        var resposta = respostaRepository.save(new Resposta(null, "Resposta Outro Usuário", this.topico, LocalDateTime.now(), autorResposta, false));

        // Act & Assert
        mvc.perform(
                delete("/topicos/{idTopico}/respostas/{idResposta}", this.topico.getId(), resposta.getId())
                        .with(user(this.autor))
        ).andExpect(status().isNoContent());
    }
}
