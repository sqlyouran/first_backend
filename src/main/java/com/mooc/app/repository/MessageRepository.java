package com.mooc.app.repository;

import com.mooc.app.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    @Query("SELECT m FROM MessageEntity m WHERE m.conversationId = :conversationId AND m.deleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<MessageEntity> findByConversation(@Param("conversationId") UUID conversationId, Pageable pageable);

    long countByConversationIdAndSenderIdAndReadFalse(UUID conversationId, UUID senderId);

    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE " +
           "m.conversationId = :conversationId AND m.senderId = :senderId AND " +
           "m.createdAt > :since AND m.deleted = false")
    long countRecentMessages(@Param("conversationId") UUID conversationId,
                             @Param("senderId") UUID senderId,
                             @Param("since") Instant since);

    @Modifying
    @Query("UPDATE MessageEntity m SET m.read = true, m.updatedAt = :now WHERE " +
           "m.conversationId = :conversationId AND m.senderId = :senderId AND m.read = false AND m.deleted = false")
    int markAllAsRead(@Param("conversationId") UUID conversationId,
                      @Param("senderId") UUID senderId,
                      @Param("now") Instant now);

    @Query("SELECT m FROM MessageEntity m WHERE m.id IN " +
           "(SELECT MAX(m2.id) FROM MessageEntity m2 WHERE m2.conversationId IN :convIds AND m2.deleted = false GROUP BY m2.conversationId)")
    List<MessageEntity> findLatestMessagesByConversationIds(@Param("convIds") List<UUID> convIds);

    @Query("SELECT m.conversationId, COUNT(m) FROM MessageEntity m " +
           "WHERE m.conversationId IN :convIds AND m.senderId IN :otherUserIds AND m.read = false AND m.deleted = false " +
           "GROUP BY m.conversationId")
    List<Object[]> batchCountUnread(@Param("convIds") List<UUID> convIds, @Param("otherUserIds") List<UUID> otherUserIds);

    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE " +
           "m.conversationId IN (SELECT c.id FROM ConversationEntity c WHERE c.userAId = :userId OR c.userBId = :userId) " +
           "AND m.senderId != :userId AND m.read = false AND m.deleted = false")
    long countTotalUnread(@Param("userId") UUID userId);
}
