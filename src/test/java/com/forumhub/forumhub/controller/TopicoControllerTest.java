package com.forumhub.forumhub.controller;

import com.forumhub.forumhub.repository.UsuarioRepository;
import com.forumhub.forumhub.dto.topico.DadosAtualizacaoTopico;
import com.forumhub.forumhub.dto.topico.DadosCadastroTopico;
import com.forumhub.forumhub.model.StatusTopico;
import com.forumhub.forumhub.model.Usuario;
import com.forumhub.forumhub.model.Topico;
import com.forumhub.forumhub.repository.TopicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@ActiveProfiles("test")
class TopicoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JacksonTester<DadosCadastroTopico> dadosCadastroJson;

    @Autowired
    private JacksonTester<DadosAtualizacaoTopico> dadosAtualizacaoJson;

    @Autowired
    private TopicoRepository topicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void cleanup() {
        topicoRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioRepository.save(new Usuario(null, "teste", "senha123"));
        usuarioRepository.save(new Usuario(null, "teste.outro", "senha123"));
    }

    // Testes (Post) -> /topicos

    @Test
    @DisplayName("POST(/topicos) - Deve retornar http 400 (Bad request) devido a dados inválidoss")
    @WithMockUser
    void cadastrar_cenario1() throws Exception {
        mvc.perform(post("/topicos")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST(/topicos) - Deve retornar http 201 (Created) devido a dados válidos")
    @WithMockUser
    void cadastrar_cenario2() throws Exception {
        var dadosCadastro = new DadosCadastroTopico("Dúvida sobre Testes", "Como faço para testar?", "Testador1", "Spring Boot");
        mvc.perform(post("/topicos").contentType(MediaType.APPLICATION_JSON).characterEncoding("UTF-8").content(dadosCadastroJson.write(dadosCadastro).getJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Dúvida sobre Testes"));
    }

    @Test
    @DisplayName("POST(/topicos) - Deve retornar http 400 (Bad Request) se cadastrar título em branco")
    @WithMockUser
    void cadastrar_cenario4_tituloEmBranco() throws Exception {
        // Arrange
        var dadosComTituloInvalido = new DadosCadastroTopico(
                "",
                "Mensagem válida",
                "Autor Válido",
                "Curso Válido"
        );

        // Act & Assert
        mvc.perform(post("/topicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosCadastroJson.write(dadosComTituloInvalido).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST(/topicos) - Deve retornar http 409 (Conflict) devido a tópico duplicado")
    @WithMockUser
    void cadastrar_cenario3() throws Exception {
        var dadosCadastro = new DadosCadastroTopico("Tópico com este nome já existe!", "Mensagem repetida.", "Teste2", "Java");
        topicoRepository.save(new Topico(dadosCadastro));
        var response = mvc.perform(post("/topicos").contentType(MediaType.APPLICATION_JSON).content(dadosCadastroJson.write(dadosCadastro).getJson())).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    // Testes (get) -> /topicos/{id}

    @Test
    @DisplayName("GET /topicos/{id} - Deveria retornar 200 e detalhar o tópico para ID existente e ativo")
    @WithMockUser
    void detalhar_cenario1() throws Exception {
        var topico = criarTopicoPadraoNoBanco();
        mvc.perform(get("/topicos/" + topico.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(topico.getId()))
                .andExpect(jsonPath("$.titulo").value(topico.getTitulo()));
    }

    @Test
    @DisplayName("GET /topicos/{id} - Deveria retornar 404 para ID que não existe")
    @WithMockUser
    void detalhar_cenario2() throws Exception {
        mvc.perform(get("/topicos/999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /topicos - Deveria retornar 200 e uma lista paginada de tópicos")
    @WithMockUser
    void listar_cenario3() throws Exception {
        // Arrange
        topicoRepository.save(new Topico(new DadosCadastroTopico("Tópico 1", "...", "Autor A", "Curso X")));
        topicoRepository.save(new Topico(new DadosCadastroTopico("Tópico 2", "...", "Autor B", "Curso Y")));

        // Act & Assert
        mvc.perform(get("/topicos")
                        .param("size", "1")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].titulo").value("Tópico 1"));
    }

    // Testes (put) -> /topicos/{id}

    @Test
    @DisplayName("PUT(/topicos/{id}) - Deve retornar http 200 (OK) e atualizar o tópico")
    @WithUserDetails("Teste3")
    void atualizar_cenario1() throws Exception {
        var topico = topicoRepository.save(new Topico(new DadosCadastroTopico(
                "Dúvida sobre Testes", "Como criar testes?", "Teste3", "Software"
        )));

        var dadosAtualizacao = new DadosAtualizacaoTopico(
                "Título Atualizado",
                "Mensagem atualizada com sucesso!",
                StatusTopico.SOLUCIONADO
        );

        mvc.perform(put("/topicos/" + topico.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosAtualizacaoJson.write(dadosAtualizacao).getJson())
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT(/topicos/{id}) - Deve retornar http 404 (Not found) se tentar atualizar ID não existente")
    @WithMockUser
    void atualizar_cenario2() throws Exception {
        var dadosAtualizacao = new DadosAtualizacaoTopico("Título exemplo", "Mensagem exemplo", null);
        mvc.perform(put("/topicos/999").contentType(MediaType.APPLICATION_JSON).content(dadosAtualizacaoJson.write(dadosAtualizacao).getJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT(/topicos/{id}) - Deve retornar http 403 (Forbidden) se tentar atualizar tópico de outro usuário")
    @WithUserDetails("tentando.invadir")
    void atualizar_cenario3_outroUsuario() throws Exception {
        // Arrange
        var dadosCadastro = new DadosCadastroTopico("Tópico Original", "Minha mensagem", "Teste4", "Java");
        var topico = topicoRepository.save(new Topico(dadosCadastro));

        var dadosAtualizacao = new DadosAtualizacaoTopico("Título Inválido", "Mensagem Inválida", null);

        // Act & Assert
        mvc.perform(put("/topicos/" + topico.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosAtualizacaoJson.write(dadosAtualizacao).getJson()))
                .andExpect(status().isForbidden());
    }

    // Testes (delete) -> /topicos/{id}

    @Test
    @DisplayName("DELETE(/topicos/{id}) - Deve retornar http 204 (No content) para exclusão de ID existente")
    @WithUserDetails("Teste1")
    void excluir_cenario1() throws Exception {
        var topico = topicoRepository.save(new Topico(new DadosCadastroTopico(
                "Tópico a ser deletado: ", "...", "Teste1", "Testes"
        )));

        mvc.perform(delete("/topicos/" + topico.getId())).andExpect(status().isNoContent());
        var topicoInativado = topicoRepository.findById(topico.getId()).orElse(null);
        assertThat(topicoInativado).isNotNull();
        assertThat(topicoInativado.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("DELETE(/topicos/{id}) - Deve retornar http 404 (Not found) se tentar excluir ID inexistente")
    @WithMockUser
    void excluir_cenario2() throws Exception {
        mvc.perform(delete("/topicos/999")).andExpect(status().isNotFound());
    }

    private Topico criarTopicoPadraoNoBanco() {
        var dados = new DadosCadastroTopico("Dúvida sobre Testes", "Como criar testes?", "Teste1", "Software");
        var topico = new Topico(dados);
        return topicoRepository.save(topico);
    }
}
