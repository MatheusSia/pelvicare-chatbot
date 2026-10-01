package br.com.sia.chatbot.repository;

import br.com.sia.chatbot.entity.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatbotMessageRepository
        extends JpaRepository<ChatbotMessage, Long> {

    Optional<ChatbotMessage> findByInstanceAndCodeAndActiveTrue(
            String instance,
            String code
    );

    Optional<ChatbotMessage> findByInstanceAndCode(
            String instance,
            String code
    );

    boolean existsByInstanceAndCode(
            String instance,
            String code
    );

    List<ChatbotMessage> findByInstanceAndActiveTrue(
            String instance
    );
}