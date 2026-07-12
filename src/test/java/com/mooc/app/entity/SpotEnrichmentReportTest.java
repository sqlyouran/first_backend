package com.mooc.app.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SpotEnrichmentReportTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void persistAndRetrieve() {
        SpotEnrichmentReport report = new SpotEnrichmentReport();
        report.setRunId("550e8400-e29b-41d4-a716-446655440000");
        report.setStartedAt(Instant.parse("2026-07-12T02:00:00Z"));
        report.setCompletedAt(Instant.parse("2026-07-12T02:05:30Z"));
        report.setTotalAttempted(5);
        report.setTotalSuccess(4);
        report.setTotalFailed(1);
        report.setDetails("[{\"slug\":\"forbidden-city\",\"status\":\"success\"},{\"slug\":\"summer-palace\",\"status\":\"failed\",\"error\":\"page not found\"}]");

        em.persist(report);
        em.flush();

        assertNotNull(report.getId());
        assertNotNull(report.getCreatedAt());
        assertNotNull(report.getUpdatedAt());
        assertFalse(report.isDeleted());

        SpotEnrichmentReport found = em.find(SpotEnrichmentReport.class, report.getId());
        assertNotNull(found);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", found.getRunId());
        assertEquals(5, found.getTotalAttempted());
        assertEquals(4, found.getTotalSuccess());
        assertEquals(1, found.getTotalFailed());
        assertTrue(found.getDetails().contains("forbidden-city"));
    }

    @Test
    void runIdMustBeUnique() {
        SpotEnrichmentReport r1 = new SpotEnrichmentReport();
        r1.setRunId("same-run-id");
        r1.setStartedAt(Instant.now());
        r1.setCompletedAt(Instant.now());
        r1.setTotalAttempted(1);
        r1.setTotalSuccess(1);
        r1.setTotalFailed(0);
        r1.setDetails("[]");
        em.persist(r1);
        em.flush();

        SpotEnrichmentReport r2 = new SpotEnrichmentReport();
        r2.setRunId("same-run-id");
        r2.setStartedAt(Instant.now());
        r2.setCompletedAt(Instant.now());
        r2.setTotalAttempted(0);
        r2.setTotalSuccess(0);
        r2.setTotalFailed(0);
        r2.setDetails("[]");
        em.persist(r2);

        assertThrows(Exception.class, () -> em.flush());
    }
}
