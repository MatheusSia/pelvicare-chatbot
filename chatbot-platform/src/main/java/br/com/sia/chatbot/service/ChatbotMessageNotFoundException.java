package br.com.sia.chatbot.service;

public class ChatbotMessageNotFoundException
        extends RuntimeException {

    public ChatbotMessageNotFoundException(
            String instance,
            String code
    ) {
        super(
                "Mensagem do chatbot não encontrada. Instância: "
                        + instance
                        + ", código: "
                        + code
        );
    }
}