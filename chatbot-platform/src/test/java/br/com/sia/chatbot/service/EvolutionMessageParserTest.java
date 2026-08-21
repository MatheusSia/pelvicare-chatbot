package br.com.sia.chatbot.service;

import br.com.sia.chatbot.dto.EvolutionWebhookDTO;
import br.com.sia.chatbot.dto.IncomingMessageDTO;
import br.com.sia.chatbot.dto.IncomingMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EvolutionMessageParserTest {

    private EvolutionMessageParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        parser = new EvolutionMessageParser();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldParseTextMessage() throws Exception {
        JsonNode data = objectMapper.readTree(
                """
                {
                  "key": {
                    "remoteJid": "5519999999999@s.whatsapp.net",
                    "fromMe": false,
                    "id": "MESSAGE-123"
                  },
                  "pushName": "Rodrigo Sia",
                  "message": {
                    "conversation": "Olá"
                  },
                  "messageType": "conversation",
                  "messageTimestamp": 1785247200
                }
                """
        );

        EvolutionWebhookDTO webhook =
                new EvolutionWebhookDTO(
                        "messages.upsert",
                        "pelvicare",
                        data
                );

        Optional<IncomingMessageDTO> result =
                parser.parse(webhook);

        assertTrue(result.isPresent());

        IncomingMessageDTO message = result.get();

        assertEquals("pelvicare", message.instance());
        assertEquals("5519999999999", message.phone());
        assertEquals("Rodrigo Sia", message.pushName());
        assertEquals("Olá", message.text());
        assertEquals("MESSAGE-123", message.messageId());
        assertEquals(IncomingMessageType.TEXT, message.type());
        assertEquals(
                Instant.ofEpochSecond(1785247200),
                message.sentAt()
        );
    }

    @Test
    void shouldIgnoreMessageSentByConnectedNumber()
            throws Exception {

        JsonNode data = objectMapper.readTree(
                """
                {
                  "key": {
                    "remoteJid": "5519999999999@s.whatsapp.net",
                    "fromMe": true,
                    "id": "MESSAGE-123"
                  },
                  "message": {
                    "conversation": "Mensagem enviada pelo bot"
                  },
                  "messageType": "conversation",
                  "messageTimestamp": 1785247200
                }
                """
        );

        EvolutionWebhookDTO webhook =
                new EvolutionWebhookDTO(
                        "messages.upsert",
                        "pelvicare",
                        data
                );

        Optional<IncomingMessageDTO> result =
                parser.parse(webhook);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldIgnoreGroupMessage() throws Exception {
        JsonNode data = objectMapper.readTree(
                """
                {
                  "key": {
                    "remoteJid": "123456789@g.us",
                    "fromMe": false,
                    "id": "GROUP-MESSAGE-1"
                  },
                  "message": {
                    "conversation": "Mensagem no grupo"
                  },
                  "messageType": "conversation",
                  "messageTimestamp": 1785247200
                }
                """
        );

        EvolutionWebhookDTO webhook =
                new EvolutionWebhookDTO(
                        "messages.upsert",
                        "pelvicare",
                        data
                );

        Optional<IncomingMessageDTO> result =
                parser.parse(webhook);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldIdentifyAudioMessage() throws Exception {
        JsonNode data = objectMapper.readTree(
                """
                {
                  "key": {
                    "remoteJid": "5519999999999@s.whatsapp.net",
                    "fromMe": false,
                    "id": "AUDIO-123"
                  },
                  "pushName": "Rodrigo Sia",
                  "message": {
                    "audioMessage": {
                      "mimetype": "audio/ogg"
                    }
                  },
                  "messageType": "audioMessage",
                  "messageTimestamp": 1785247200
                }
                """
        );

        EvolutionWebhookDTO webhook =
                new EvolutionWebhookDTO(
                        "messages.upsert",
                        "pelvicare",
                        data
                );

        Optional<IncomingMessageDTO> result =
                parser.parse(webhook);

        assertTrue(result.isPresent());
        assertEquals(
                IncomingMessageType.AUDIO,
                result.get().type()
        );
        assertEquals(
                "[Áudio recebido]",
                result.get().text()
        );
    }

    @Test
    void shouldIgnoreDifferentWebhookEvent()
            throws Exception {

        JsonNode data = objectMapper.readTree("{}");

        EvolutionWebhookDTO webhook =
                new EvolutionWebhookDTO(
                        "connection.update",
                        "pelvicare",
                        data
                );

        Optional<IncomingMessageDTO> result =
                parser.parse(webhook);

        assertTrue(result.isEmpty());
    }
}