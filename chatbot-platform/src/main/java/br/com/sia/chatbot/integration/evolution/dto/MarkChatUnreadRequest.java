package br.com.sia.chatbot.integration.evolution.dto;

import java.util.Map;

public record MarkChatUnreadRequest(
        Map<String, Object> lastMessage,
        String chat
) {
}