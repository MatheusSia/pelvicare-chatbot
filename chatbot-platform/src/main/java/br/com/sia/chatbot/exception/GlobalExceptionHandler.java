package br.com.sia.chatbot.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(EvolutionCommunicationException.class)
    public ResponseEntity<Map<String, Object>>
    handleEvolutionCommunicationException(
            EvolutionCommunicationException exception
    ) {
        log.error(
                "Falha de comunicação com a Evolution API",
                exception
        );

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(
                        Map.of(
                                "error", "EVOLUTION_COMMUNICATION_ERROR",
                                "message", exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>>
    handleDataIntegrityViolationException(
            DataIntegrityViolationException exception
    ) {
        log.error(
                "Erro de integridade no banco de dados",
                exception
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        Map.of(
                                "error", "DATABASE_CONSTRAINT_ERROR",
                                "message",
                                "Não foi possível salvar os dados."
                        )
                );
    }

    @ExceptionHandler(ChatbotException.class)
    public ResponseEntity<Map<String, Object>>
    handleChatbotException(
            ChatbotException exception
    ) {
        log.error(
                "Erro conhecido no chatbot",
                exception
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                                "error", "CHATBOT_ERROR",
                                "message", exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleUnexpectedException(
            Exception exception
    ) {
        log.error(
                "Erro inesperado durante o processamento",
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        Map.of(
                                "error", "INTERNAL_ERROR",
                                "message",
                                "Ocorreu um erro inesperado."
                        )
                );
    }
}