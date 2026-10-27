package com.revia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.revia.domain.RegleRevue;

public interface RegleRevueRepository extends JpaRepository<RegleRevue, Long> {

    List<RegleRevue> findByUtilisateurIdOrderByCreeLeDesc(Long utilisateurId);

    Optional<RegleRevue> findByIdAndUtilisateurId(Long id, Long utilisateurId);
}
