package com.mooc.app.repository;

import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    Page<PostEntity> findByStatusAndDeletedFalse(PostStatus status, Pageable pageable);

    Page<PostEntity> findByAuthorIdAndStatusAndDeletedFalse(UUID authorId, PostStatus status, Pageable pageable);

    Optional<PostEntity> findByIdAndDeletedFalse(UUID id);

    Optional<PostEntity> findBySlugAndDeletedFalse(String slug);

    List<PostEntity> findAllByIdInAndStatusAndDeletedFalse(List<UUID> ids, PostStatus status);

    // === Cursor-based pagination (keyset) ===

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false AND p.createdAt < :cursor ORDER BY p.createdAt DESC")
    List<PostEntity> findByCreatedAtCursor(@Param("status") PostStatus status, @Param("cursor") Instant cursor, Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false AND p.createdAt < :cursor ORDER BY p.createdAt DESC")
    List<PostEntity> findByAuthorIdAndCreatedAtCursor(@Param("authorId") UUID authorId, @Param("status") PostStatus status, @Param("cursor") Instant cursor, Pageable pageable);

    // === Denormalized sorting (offset) ===

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false ORDER BY p.cachedUpVoteCount DESC, p.createdAt DESC")
    Page<PostEntity> findByUpVoteCount(Pageable pageable, @Param("status") PostStatus status);

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false ORDER BY p.cachedCommentCount DESC, p.createdAt DESC")
    Page<PostEntity> findByCommentCount(Pageable pageable, @Param("status") PostStatus status);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false ORDER BY p.cachedUpVoteCount DESC, p.createdAt DESC")
    Page<PostEntity> findByAuthorIdAndUpVoteCount(Pageable pageable, @Param("authorId") UUID authorId, @Param("status") PostStatus status);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false ORDER BY p.cachedCommentCount DESC, p.createdAt DESC")
    Page<PostEntity> findByAuthorIdAndCommentCount(Pageable pageable, @Param("authorId") UUID authorId, @Param("status") PostStatus status);

    // === Denormalized sorting (cursor / keyset) ===

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false AND ("
            + "p.cachedUpVoteCount < :cursorVotes"
            + " OR (p.cachedUpVoteCount = :cursorVotes AND p.createdAt < :cursorTime)"
            + ") ORDER BY p.cachedUpVoteCount DESC, p.createdAt DESC")
    List<PostEntity> findByUpVoteCountCursor(@Param("status") PostStatus status,
                                             @Param("cursorVotes") long cursorVotes,
                                             @Param("cursorTime") Instant cursorTime,
                                             Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false AND ("
            + "p.cachedCommentCount < :cursorComments"
            + " OR (p.cachedCommentCount = :cursorComments AND p.createdAt < :cursorTime)"
            + ") ORDER BY p.cachedCommentCount DESC, p.createdAt DESC")
    List<PostEntity> findByCommentCountCursor(@Param("status") PostStatus status,
                                              @Param("cursorComments") long cursorComments,
                                              @Param("cursorTime") Instant cursorTime,
                                              Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false AND ("
            + "p.cachedUpVoteCount < :cursorVotes"
            + " OR (p.cachedUpVoteCount = :cursorVotes AND p.createdAt < :cursorTime)"
            + ") ORDER BY p.cachedUpVoteCount DESC, p.createdAt DESC")
    List<PostEntity> findByAuthorIdAndUpVoteCountCursor(@Param("authorId") UUID authorId,
                                                        @Param("status") PostStatus status,
                                                        @Param("cursorVotes") long cursorVotes,
                                                        @Param("cursorTime") Instant cursorTime,
                                                        Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false AND ("
            + "p.cachedCommentCount < :cursorComments"
            + " OR (p.cachedCommentCount = :cursorComments AND p.createdAt < :cursorTime)"
            + ") ORDER BY p.cachedCommentCount DESC, p.createdAt DESC")
    List<PostEntity> findByAuthorIdAndCommentCountCursor(@Param("authorId") UUID authorId,
                                                         @Param("status") PostStatus status,
                                                         @Param("cursorComments") long cursorComments,
                                                         @Param("cursorTime") Instant cursorTime,
                                                         Pageable pageable);

    // === Atomic increment for denormalized counters ===

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PostEntity p SET p.cachedUpVoteCount = p.cachedUpVoteCount + :delta WHERE p.id = :postId")
    void incrementUpVoteCount(@Param("postId") UUID postId, @Param("delta") int delta);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PostEntity p SET p.cachedCommentCount = p.cachedCommentCount + :delta WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") UUID postId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE PostEntity p SET " +
            "p.cachedUpVoteCount = (SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP'), " +
            "p.cachedCommentCount = (SELECT COUNT(c) FROM CommentEntity c WHERE c.entityId = p.id AND c.entityType = 'POST' AND c.deleted = false)")
    int backfillCachedCounters();

    @Query("SELECT p FROM PostEntity p WHERE p.deleted = false AND p.status = 'PUBLISHED' AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<PostEntity> searchByKeyword(@Param("q") String query);
}
