package com.mooc.app.repository;

import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // === Cursor-based pagination (keyset) ===

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false AND p.createdAt < :cursor ORDER BY p.createdAt DESC")
    List<PostEntity> findByCreatedAtCursor(@Param("status") PostStatus status, @Param("cursor") Instant cursor, Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false AND p.createdAt < :cursor ORDER BY p.createdAt DESC")
    List<PostEntity> findByAuthorIdAndCreatedAtCursor(@Param("authorId") UUID authorId, @Param("status") PostStatus status, @Param("cursor") Instant cursor, Pageable pageable);

    // === Aggregation-based sorting (offset only) ===

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false ORDER BY (SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP') DESC, p.createdAt DESC")
    Page<PostEntity> findByUpVoteCount(Pageable pageable, @Param("status") PostStatus status);

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false ORDER BY (SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = p.id AND c.deleted = false) DESC, p.createdAt DESC")
    Page<PostEntity> findByCommentCount(Pageable pageable, @Param("status") PostStatus status);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false ORDER BY (SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP') DESC, p.createdAt DESC")
    Page<PostEntity> findByAuthorIdAndUpVoteCount(Pageable pageable, @Param("authorId") UUID authorId, @Param("status") PostStatus status);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false ORDER BY (SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = p.id AND c.deleted = false) DESC, p.createdAt DESC")
    Page<PostEntity> findByAuthorIdAndCommentCount(Pageable pageable, @Param("authorId") UUID authorId, @Param("status") PostStatus status);

    // === Aggregation-based sorting (cursor / keyset) ===

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false AND ("
            + "(SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP') < :cursorVotes"
            + " OR ((SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP') = :cursorVotes AND p.createdAt < :cursorTime)"
            + ") ORDER BY (SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP') DESC, p.createdAt DESC")
    List<PostEntity> findByUpVoteCountCursor(@Param("status") PostStatus status,
                                             @Param("cursorVotes") long cursorVotes,
                                             @Param("cursorTime") Instant cursorTime,
                                             Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.status = :status AND p.deleted = false AND ("
            + "(SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = p.id AND c.deleted = false) < :cursorComments"
            + " OR ((SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = p.id AND c.deleted = false) = :cursorComments AND p.createdAt < :cursorTime)"
            + ") ORDER BY (SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = p.id AND c.deleted = false) DESC, p.createdAt DESC")
    List<PostEntity> findByCommentCountCursor(@Param("status") PostStatus status,
                                              @Param("cursorComments") long cursorComments,
                                              @Param("cursorTime") Instant cursorTime,
                                              Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false AND ("
            + "(SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP') < :cursorVotes"
            + " OR ((SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP') = :cursorVotes AND p.createdAt < :cursorTime)"
            + ") ORDER BY (SELECT COUNT(v) FROM VoteEntity v WHERE v.postId = p.id AND v.voteType = 'UP') DESC, p.createdAt DESC")
    List<PostEntity> findByAuthorIdAndUpVoteCountCursor(@Param("authorId") UUID authorId,
                                                        @Param("status") PostStatus status,
                                                        @Param("cursorVotes") long cursorVotes,
                                                        @Param("cursorTime") Instant cursorTime,
                                                        Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.authorId = :authorId AND p.status = :status AND p.deleted = false AND ("
            + "(SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = p.id AND c.deleted = false) < :cursorComments"
            + " OR ((SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = p.id AND c.deleted = false) = :cursorComments AND p.createdAt < :cursorTime)"
            + ") ORDER BY (SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = p.id AND c.deleted = false) DESC, p.createdAt DESC")
    List<PostEntity> findByAuthorIdAndCommentCountCursor(@Param("authorId") UUID authorId,
                                                         @Param("status") PostStatus status,
                                                         @Param("cursorComments") long cursorComments,
                                                         @Param("cursorTime") Instant cursorTime,
                                                         Pageable pageable);
}
