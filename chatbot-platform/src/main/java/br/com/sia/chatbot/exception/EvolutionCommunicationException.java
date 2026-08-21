package br.com.sia.chatbot.exception;

public class EvolutionCommunicationException
        extends ChatbotException {

    public EvolutionCommunicationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}