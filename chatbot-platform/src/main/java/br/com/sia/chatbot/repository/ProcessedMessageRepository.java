package br.com.sia.chatbot.repository;

import br.com.sia.chatbot.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessedMessageRepository
        extends JpaRepository<ProcessedMessage, Long> {

    boolean existsByInstanceAndMessageId(
            String instance,
            String messageId
    );

    @Transactional
    void deleteByInstanceAndMessageId(
            String instance,
            String messageId
    );
}