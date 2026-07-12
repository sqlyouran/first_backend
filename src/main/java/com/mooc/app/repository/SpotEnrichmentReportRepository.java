package com.mooc.app.repository;

import com.mooc.app.entity.SpotEnrichmentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpotEnrichmentReportRepository extends JpaRepository<SpotEnrichmentReport, UUID> {

    Optional<SpotEnrichmentReport> findTopByOrderByStartedAtDesc();
}
