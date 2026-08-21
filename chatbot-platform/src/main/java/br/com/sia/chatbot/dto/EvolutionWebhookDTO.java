package br.com.sia.chatbot.dto;

import tools.jackson.databind.JsonNode;

public record EvolutionWebhookDTO(
        String event,
        String instance,
        JsonNode data
) {
}