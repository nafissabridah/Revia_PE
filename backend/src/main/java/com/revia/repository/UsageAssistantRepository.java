package com.revia.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.revia.domain.UsageAssistant;

public interface UsageAssistantRepository extends JpaRepository<UsageAssistant, Long> {

    long countByUtilisateurIdAndCreeLeGreaterThanEqual(Long utilisateurId, LocalDateTime depuis);

    List<UsageAssistant> findByCreeLeGreaterThanEqual(LocalDateTime depuis);
}
