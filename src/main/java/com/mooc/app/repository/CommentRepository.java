package com.mooc.app.repository;

import com.mooc.app.entity.CommentEntity;
import com.mooc.app.entity.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    @Query("SELECT c FROM CommentEntity c WHERE c.entityId = :entityId AND c.entityType = :entityType AND c.parentCommentId IS NULL ORDER BY c.createdAt ASC")
    Page<CommentEntity> findTopLevelByEntity(@Param("entityId") UUID entityId, @Param("entityType") EntityType entityType, Pageable pageable);

    @Query("SELECT c FROM CommentEntity c WHERE c.parentCommentId = :parentId ORDER BY c.createdAt ASC")
    Page<CommentEntity> findRepliesByParentId(@Param("parentId") UUID parentId, Pageable pageable);

    Optional<CommentEntity> findByIdAndDeletedFalse(UUID id);

    @Query("SELECT c.entityId, COUNT(c) FROM CommentEntity c WHERE c.entityId IN :entityIds AND c.entityType = :entityType AND c.deleted = false GROUP BY c.entityId")
    List<Object[]> batchCountActiveComments(@Param("entityIds") List<UUID> entityIds, @Param("entityType") EntityType entityType);
}
