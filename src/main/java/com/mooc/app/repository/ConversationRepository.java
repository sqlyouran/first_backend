package com.mooc.app.repository;

import com.mooc.app.entity.ConversationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    @Query("SELECT c FROM ConversationEntity c WHERE " +
           "(c.userAId = :userId1 AND c.userBId = :userId2) OR " +
           "(c.userAId = :userId2 AND c.userBId = :userId1)")
    Optional<ConversationEntity> findBetweenUsers(@Param("userId1") UUID userId1,
                                                   @Param("userId2") UUID userId2);

    @Query("SELECT c FROM ConversationEntity c WHERE " +
           "(c.userAId = :userId OR c.userBId = :userId) AND c.deleted = false " +
           "ORDER BY c.lastMessageAt DESC")
    Page<ConversationEntity> findByUser(@Param("userId") UUID userId, Pageable pageable);
}
