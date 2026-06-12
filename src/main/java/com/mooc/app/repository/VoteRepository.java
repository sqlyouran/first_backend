package com.mooc.app.repository;

import com.mooc.app.entity.VoteEntity;
import com.mooc.app.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<VoteEntity, UUID> {

    Optional<VoteEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostIdAndVoteType(UUID postId, VoteType voteType);

    @Query("SELECT v.postId, COUNT(v) FROM VoteEntity v WHERE v.postId IN :postIds AND v.voteType = 'UP' GROUP BY v.postId")
    List<Object[]> batchCountUpVotes(@Param("postIds") List<UUID> postIds);
}
