package com.forumhub.forumhub.controller;

import com.forumhub.forumhub.dto.resposta.DadosAtualizacaoResposta;
import com.forumhub.forumhub.dto.resposta.DadosCadastroResposta;
import com.forumhub.forumhub.dto.resposta.DadosDetalhamentoResposta;
import com.forumhub.forumhub.dto.resposta.DadosListagemResposta;
import com.forumhub.forumhub.infra.exception.ValidacaoException;
import com.forumhub.forumhub.model.Resposta;
import com.forumhub.forumhub.model.Usuario;
import com.forumhub.forumhub.repository.RespostaRepository;
import com.forumhub.forumhub.repository.TopicoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/topicos/{idTopico}/respostas")
public class RespostaController {

    @Autowired
    private RespostaRepository respostaRepository;

    @Autowired
    private TopicoRepository topicoRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<DadosDetalhamentoResposta> criarResposta(
            @PathVariable Long idTopico,
            @RequestBody @Valid DadosCadastroResposta dados,
            @AuthenticationPrincipal Usuario autor,
            UriComponentsBuilder uriBuilder) {

        var topico = topicoRepository.findById(idTopico)
                .orElseThrow(() -> new EntityNotFoundException("Tópico ID" + idTopico + " não encontrado!"));

        var resposta = new Resposta(null, dados.mensagem(), topico, LocalDateTime.now(), autor, false);
        respostaRepository.save(resposta);

        var uri = uriBuilder.path("/topicos/{idTopico}/respostas/{idResposta}").buildAndExpand(idTopico, resposta.getId()).toUri();
        return ResponseEntity.created(uri).body(new DadosDetalhamentoResposta(resposta));
    }

    @GetMapping
    public ResponseEntity<List<DadosListagemResposta>> listarRespostas(@PathVariable Long idTopico) {
        if (!topicoRepository.existsById(idTopico)) {
            throw new EntityNotFoundException("Tópico ID " + idTopico + " não encontrado!");
        }

        var respostas = respostaRepository.findByTopicoId(idTopico);

        var listaDto = respostas.stream()
                .map(DadosListagemResposta::new)
                .toList();

        return ResponseEntity.ok(listaDto);
    }

    @PutMapping("/{idResposta}")
    @Transactional
    public ResponseEntity<DadosDetalhamentoResposta> atualizarResposta(
            @PathVariable Long idTopico,
            @PathVariable Long idResposta,
            @RequestBody @Valid DadosAtualizacaoResposta dados,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        var resposta = respostaRepository.findById(idResposta)
                .orElseThrow(() -> new EntityNotFoundException("Resposta ID " + idResposta + " não encontrada!"));

        if (!resposta.getTopico().getId().equals(idTopico)) {
            throw new ValidacaoException("Esta resposta não pertence ao tópico informado!");
        }

        if (!resposta.getAutor().equals(usuarioLogado)) {
            throw new ValidacaoException("Apenas o Autor pode editar esta resposta.");
        }

        resposta.atualizarInformacoes(dados.mensagem());

        return ResponseEntity.ok(new DadosDetalhamentoResposta(resposta));
    }

    @DeleteMapping("/{idResposta}")
    @Transactional
    public ResponseEntity<Void> deletarResposta(
            @PathVariable Long idTopico,
            @PathVariable Long idResposta,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        var resposta = respostaRepository.findById(idResposta)
                .orElseThrow(() -> new EntityNotFoundException("Resposta ID " + idResposta + " não encontrada!"));

        if (!resposta.getTopico().getId().equals(idTopico)) {
            throw new ValidacaoException("Esta resposta não pertence ao tópico informado.");
        }

        var autorDoTopico = resposta.getTopico().getAutor();
        if (!resposta.getAutor().equals(usuarioLogado) && !autorDoTopico.equals(usuarioLogado.getUsername())) {
            throw new ValidacaoException("Apenas o Autor da resposta/tópico pode excluir.");
        }

        respostaRepository.delete(resposta);

        return ResponseEntity.noContent().build();
    }
}