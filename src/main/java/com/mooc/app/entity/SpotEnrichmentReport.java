package com.mooc.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "spot_enrichment_reports")
public class SpotEnrichmentReport extends BaseEntity {

    @NotBlank(message = "Run ID must not be blank")
    @Size(max = 36)
    @Column(name = "run_id", nullable = false, unique = true, length = 36)
    private String runId;

    @NotNull(message = "Started at must not be null")
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @NotNull(message = "Completed at must not be null")
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "total_attempted")
    private int totalAttempted;

    @Column(name = "total_success")
    private int totalSuccess;

    @Column(name = "total_failed")
    private int totalFailed;

    @Column(columnDefinition = "TEXT")
    private String details;

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public int getTotalAttempted() { return totalAttempted; }
    public void setTotalAttempted(int totalAttempted) { this.totalAttempted = totalAttempted; }

    public int getTotalSuccess() { return totalSuccess; }
    public void setTotalSuccess(int totalSuccess) { this.totalSuccess = totalSuccess; }

    public int getTotalFailed() { return totalFailed; }
    public void setTotalFailed(int totalFailed) { this.totalFailed = totalFailed; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
