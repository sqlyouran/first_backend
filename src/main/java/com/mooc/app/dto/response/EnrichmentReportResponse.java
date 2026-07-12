package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class EnrichmentReportResponse extends BaseResponse {

    @JsonProperty("run_id")
    private final String runId;
    @JsonProperty("started_at")
    private final Instant startedAt;
    @JsonProperty("completed_at")
    private final Instant completedAt;
    @JsonProperty("total_attempted")
    private final int totalAttempted;
    @JsonProperty("total_success")
    private final int totalSuccess;
    @JsonProperty("total_failed")
    private final int totalFailed;
    private final String details;

    public EnrichmentReportResponse(String requestId, String runId, Instant startedAt,
                                     Instant completedAt, int totalAttempted,
                                     int totalSuccess, int totalFailed, String details) {
        super(requestId);
        this.runId = runId;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.totalAttempted = totalAttempted;
        this.totalSuccess = totalSuccess;
        this.totalFailed = totalFailed;
        this.details = details;
    }

    public String getRunId() { return runId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public int getTotalAttempted() { return totalAttempted; }
    public int getTotalSuccess() { return totalSuccess; }
    public int getTotalFailed() { return totalFailed; }
    public String getDetails() { return details; }
}
