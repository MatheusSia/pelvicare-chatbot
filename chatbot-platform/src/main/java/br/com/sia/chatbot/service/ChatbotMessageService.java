package br.com.sia.chatbot.service;

import br.com.sia.chatbot.entity.ChatbotMessage;
import br.com.sia.chatbot.repository.ChatbotMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatbotMessageService {

    private final ChatbotMessageRepository repository;

    public ChatbotMessageService(
            ChatbotMessageRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public String getMessage(
            String instance,
            String code
    ) {
        return repository
                .findByInstanceAndCodeAndActiveTrue(
                        instance,
                        code
                )
                .map(ChatbotMessage::getContent)
                .orElseThrow(
                        () -> new ChatbotMessageNotFoundException(
                                instance,
                                code
                        )
                );
    }
}