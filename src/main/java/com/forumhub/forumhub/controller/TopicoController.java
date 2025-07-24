package com.forumhub.forumhub.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.forumhub.forumhub.dto.topico.*;
import com.forumhub.forumhub.model.Topico;
import com.forumhub.forumhub.model.Usuario;
import com.forumhub.forumhub.repository.TopicoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/topicos")
public class TopicoController {

    @Autowired
    private TopicoRepository repository;

    @PostMapping
    @Transactional
    public ResponseEntity cadastrar(@RequestBody @Valid DadosCadastroTopico dados, UriComponentsBuilder uriBuilder) {
        if (repository.existsByTituloAndMensagem(dados.titulo(), dados.mensagem())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Tópico duplicado! Tente criar um tópico com título e mensagem diferentes!");
        }

        var topico = new Topico(dados);
        repository.save(topico);

        var uri = uriBuilder.path("/topicos/{id}").buildAndExpand(topico.getId()).toUri();
        return ResponseEntity.created(uri).body(topico);
    }

    @GetMapping
    public ResponseEntity<Page<DadosListagemTopico>> listar(
            @PageableDefault(size = 10, sort = {"dataCriacao"}) Pageable paginacao,
            @RequestParam(required = false) String curso,
            @RequestParam(required = false) Integer ano) {

        Specification<Topico> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isTrue(root.get("ativo")));

            if (curso != null && !curso.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("curso"), curso));
            }

            if (ano != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function("YEAR", Integer.class, root.get("dataCriacao")), ano
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        var page = repository.findAll(spec, paginacao).map(DadosListagemTopico::new);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DadosDetalhamentoTopico> detalhar(@PathVariable Long id) {
        var topico = repository.findAtivoById(id)
                .orElseThrow(EntityNotFoundException::new);
        return ResponseEntity.ok(new DadosDetalhamentoTopico(topico));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<DadosDetalhamentoTopico> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid DadosAtualizacaoTopico dados,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        var topico = repository.findAtivoById(id)
                .orElseThrow(EntityNotFoundException::new);

        if (!topico.getAutor().equalsIgnoreCase(usuarioLogado.getUsername())) {
            throw new AccessDeniedException("Acesso negado. Apenas o autor deste tópico pode acessá-lo.");
        }

        repository.findByTituloAndMensagem(dados.titulo(), dados.mensagem())
                .ifPresent(t -> {
                    if (!t.getId().equals(id)) {
                        throw new ValidationException("Tópico duplicado! Já existe um tópico com este título e mensagem!");
                    }
                });
        topico.atualizarInformacoes(dados);

        return ResponseEntity.ok(new DadosDetalhamentoTopico(topico));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        var topico = repository.findAtivoById(id).orElseThrow(EntityNotFoundException::new);

        if (!topico.getAutor().equalsIgnoreCase(usuarioLogado.getUsername())) {
            throw new AccessDeniedException("Acesso negado. Apenas o autor deste tópico pode acessá-lo.");
        }
        topico.excluir();

        return ResponseEntity.noContent().build();
    }
}
