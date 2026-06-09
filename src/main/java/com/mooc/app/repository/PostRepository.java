package com.mooc.app.repository;

import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    Page<PostEntity> findByStatusAndDeletedFalse(PostStatus status, Pageable pageable);

    Page<PostEntity> findByAuthorIdAndStatusAndDeletedFalse(UUID authorId, PostStatus status, Pageable pageable);

    Optional<PostEntity> findByIdAndDeletedFalse(UUID id);
}
