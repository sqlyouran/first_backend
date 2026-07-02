package com.mooc.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import static org.mockito.Mockito.*;

class KnowledgeIndexSchedulerTest {

    @Test
    void onApplicationReady_triggersAsyncRebuild() {
        // GIVEN
        KnowledgeBuilderService builderService = mock(KnowledgeBuilderService.class);
        KnowledgeIndexScheduler scheduler = new KnowledgeIndexScheduler(builderService);

        // WHEN
        scheduler.onApplicationReady();

        // THEN
        verify(builderService).rebuildAllAsync();
    }

    @Test
    void scheduledRebuild_triggersSyncRebuild() {
        // GIVEN
        KnowledgeBuilderService builderService = mock(KnowledgeBuilderService.class);
        KnowledgeIndexScheduler scheduler = new KnowledgeIndexScheduler(builderService);

        // WHEN
        scheduler.scheduledRebuild();

        // THEN
        verify(builderService).rebuildAll();
    }
}
