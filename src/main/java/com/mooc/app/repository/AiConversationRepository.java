package com.mooc.app.repository;

import com.mooc.app.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {
}
