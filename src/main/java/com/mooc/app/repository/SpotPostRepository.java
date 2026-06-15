package com.mooc.app.repository;

import com.mooc.app.entity.SpotPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpotPostRepository extends JpaRepository<SpotPostEntity, UUID> {

    Page<SpotPostEntity> findBySpotIdAndDeletedFalse(UUID spotId, Pageable pageable);

    List<SpotPostEntity> findByPostIdAndDeletedFalse(UUID postId);

    boolean existsBySpotIdAndPostIdAndDeletedFalse(UUID spotId, UUID postId);
}
