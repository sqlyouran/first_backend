package com.mooc.app.repository;

import com.mooc.app.entity.BookmarkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<BookmarkEntity, UUID> {

    Optional<BookmarkEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    Page<BookmarkEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
