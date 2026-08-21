package br.com.sia.chatbot.repository;

import br.com.sia.chatbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import br.com.sia.chatbot.entity.MessageDirection;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(
            Long conversationId
    );

    boolean existsByInstanceAndMessageIdAndDirection(
            String instance,
            String messageId,
            MessageDirection direction
    );
}