package com.revia.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.revia.domain.UsageAssistant;

public interface UsageAssistantRepository extends JpaRepository<UsageAssistant, Long> {

    long countByUtilisateurIdAndCreeLeGreaterThanEqual(Long utilisateurId, LocalDateTime depuis);
}
