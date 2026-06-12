package com.mooc.app.repository;

import com.mooc.app.entity.BookmarkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<BookmarkEntity, UUID> {

    Optional<BookmarkEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    Page<BookmarkEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT b.postId, COUNT(b) FROM BookmarkEntity b WHERE b.postId IN :postIds GROUP BY b.postId")
    List<Object[]> batchCountBookmarks(@Param("postIds") List<UUID> postIds);
}
