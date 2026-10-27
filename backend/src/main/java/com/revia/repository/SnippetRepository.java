package com.revia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.revia.domain.Snippet;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    List<Snippet> findByUtilisateurIdOrderByModifieLeDesc(Long utilisateurId);

    Optional<Snippet> findByIdAndUtilisateurId(Long id, Long utilisateurId);
}
