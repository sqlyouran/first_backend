package com.mooc.app.config;

import com.mooc.app.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfills cachedUpVoteCount and cachedCommentCount for existing posts.
 * Safe to run multiple times (idempotent UPDATE).
 */
@Component
public class DataMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);

    private final PostRepository postRepository;

    public DataMigrationRunner(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting denormalized counter backfill...");
        int updated = postRepository.backfillCachedCounters();
        log.info("Counter backfill complete. {} posts updated.", updated);
    }
}
