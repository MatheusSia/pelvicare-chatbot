package br.com.sia.chatbot.config;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApplicationStartupTime {

    private final Instant startedAt = Instant.now();

    public Instant getStartedAt() {
        return startedAt;
    }
}