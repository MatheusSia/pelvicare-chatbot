package br.com.sia.chatbot.service;

import br.com.sia.chatbot.dto.IncomingMessageDTO;
import br.com.sia.chatbot.entity.ChatMessage;
import br.com.sia.chatbot.entity.Conversation;
import br.com.sia.chatbot.entity.MessageDirection;
import br.com.sia.chatbot.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChatMessageService {

    private final ChatMessageRepository repository;

    public ChatMessageService(
            ChatMessageRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public void saveInbound(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        boolean alreadyExists =
                repository.existsByInstanceAndMessageIdAndDirection(
                        message.instance(),
                        message.messageId(),
                        MessageDirection.INBOUND
                );

        if (alreadyExists) {
            return;
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversation(conversation);
        chatMessage.setInstance(message.instance());
        chatMessage.setMessageId(message.messageId());
        chatMessage.setDirection(MessageDirection.INBOUND);
        chatMessage.setContent(message.text());
        chatMessage.setSenderName(message.pushName());

        chatMessage.setCreatedAt(
                message.sentAt() != null
                        ? message.sentAt()
                        : Instant.now()
        );

        repository.save(chatMessage);
    }

    @Transactional
    public void saveClinicOutbound(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        boolean alreadyExists =
                repository.existsByInstanceAndMessageIdAndDirection(
                        message.instance(),
                        message.messageId(),
                        MessageDirection.OUTBOUND
                );

        if (alreadyExists) {
            return;
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversation(conversation);
        chatMessage.setInstance(message.instance());
        chatMessage.setMessageId(message.messageId());
        chatMessage.setDirection(MessageDirection.OUTBOUND);
        chatMessage.setContent(message.text());
        chatMessage.setSenderName("Clínica");
        chatMessage.setCreatedAt(
                message.sentAt() != null
                        ? message.sentAt()
                        : Instant.now()
        );

        repository.save(chatMessage);
    }

    @Transactional
    public void saveOutbound(
            Conversation conversation,
            String instance,
            String text
    ) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversation(conversation);
        chatMessage.setInstance(instance);

        /*
         * Como a Evolution ainda não está retornando o ID da mensagem
         * enviada no método atual, geramos um identificador interno.
         */
        chatMessage.setMessageId(
                "internal-" + UUID.randomUUID()
        );

        chatMessage.setDirection(MessageDirection.OUTBOUND);
        chatMessage.setContent(text);
        chatMessage.setSenderName("Chatbot");
        chatMessage.setCreatedAt(Instant.now());

        repository.save(chatMessage);
    }
}