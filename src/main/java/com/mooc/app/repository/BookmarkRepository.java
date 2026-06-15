package com.mooc.app.repository;

import com.mooc.app.entity.BookmarkEntity;
import com.mooc.app.entity.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<BookmarkEntity, UUID> {

    Optional<BookmarkEntity> findByEntityIdAndEntityTypeAndUserId(UUID entityId, EntityType entityType, UUID userId);

    Page<BookmarkEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<BookmarkEntity> findByUserIdAndEntityTypeOrderByCreatedAtDesc(UUID userId, EntityType entityType, Pageable pageable);

    @Query("SELECT b.entityId, COUNT(b) FROM BookmarkEntity b WHERE b.entityId IN :entityIds AND b.entityType = :entityType GROUP BY b.entityId")
    List<Object[]> batchCountBookmarks(@Param("entityIds") List<UUID> entityIds, @Param("entityType") EntityType entityType);
}
