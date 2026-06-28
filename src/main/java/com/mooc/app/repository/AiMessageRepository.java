package com.mooc.app.repository;

import com.mooc.app.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    List<AiMessage> findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(UUID conversationId);
}
