package com.mooc.app.repository;

import com.mooc.app.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentCommentId IS NULL ORDER BY c.createdAt ASC")
    Page<CommentEntity> findTopLevelByPostId(@Param("postId") UUID postId, Pageable pageable);

    @Query("SELECT c FROM CommentEntity c WHERE c.parentCommentId = :parentId ORDER BY c.createdAt ASC")
    Page<CommentEntity> findRepliesByParentId(@Param("parentId") UUID parentId, Pageable pageable);

    Optional<CommentEntity> findByIdAndDeletedFalse(UUID id);
}
