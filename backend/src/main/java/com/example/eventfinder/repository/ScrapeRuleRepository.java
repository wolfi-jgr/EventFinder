package com.example.eventfinder.repository;

import com.example.eventfinder.model.ScrapeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapeRuleRepository extends JpaRepository<ScrapeRule, Long> {
    Optional<ScrapeRule> findBySiteName(String siteName);
    List<ScrapeRule> findByEnabledTrue();
}
