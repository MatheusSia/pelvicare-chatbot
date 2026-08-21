package br.com.sia.chatbot.dto;

public record SendTextRequest(
        String number,
        String text
) {
}