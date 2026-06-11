package com.mooc.app.repository;

import com.mooc.app.entity.VoteEntity;
import com.mooc.app.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<VoteEntity, UUID> {

    Optional<VoteEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostIdAndVoteType(UUID postId, VoteType voteType);
}
