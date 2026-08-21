package br.com.sia.chatbot.repository;

import br.com.sia.chatbot.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByInstanceAndPhone(
            String instance,
            String phone
    );
}