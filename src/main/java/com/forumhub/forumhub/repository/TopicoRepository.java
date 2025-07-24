package com.forumhub.forumhub.repository;

import com.forumhub.forumhub.model.Topico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TopicoRepository extends JpaRepository<Topico, Long>, JpaSpecificationExecutor<Topico> {
    boolean existsByTituloAndMensagem(String titulo, String mensagem);

    Optional<Topico> findByTituloAndMensagem(String titulo, String mensagem);

    Page<Topico> findAllByAtivoTrue(Pageable paginacao);

    @Query("""
            SELECT t FROM Topico t WHERE t.id = :id AND t.ativo = true
            """)
    Optional<Topico> findAtivoById(Long id);
}