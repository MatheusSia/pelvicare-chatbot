package br.com.sia.chatbot.dto;

import java.time.Instant;

public record IncomingMessageDTO(
        String instance,
        String phone,
        String pushName,
        String text,
        String messageId,
        Instant sentAt,
        IncomingMessageType type,
        boolean fromMe
) {
}