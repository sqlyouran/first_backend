package com.mooc.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeIndexScheduler {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexScheduler.class);

    private final KnowledgeBuilderService knowledgeBuilderService;

    public KnowledgeIndexScheduler(KnowledgeBuilderService knowledgeBuilderService) {
        this.knowledgeBuilderService = knowledgeBuilderService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready, triggering initial knowledge base build");
        knowledgeBuilderService.rebuildAllAsync();
    }

    @Scheduled(cron = "${app.rag.rebuild-cron:0 0 */6 * * *}")
    public void scheduledRebuild() {
        log.info("Scheduled knowledge base rebuild triggered");
        knowledgeBuilderService.rebuildAll();
    }
}
