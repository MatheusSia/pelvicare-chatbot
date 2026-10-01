package br.com.sia.chatbot.service;

import br.com.sia.chatbot.dto.EvolutionWebhookDTO;
import br.com.sia.chatbot.dto.IncomingMessageDTO;
import br.com.sia.chatbot.dto.IncomingMessageType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Optional;

@Service
public class EvolutionMessageParser {

    public Optional<IncomingMessageDTO> parse(
            EvolutionWebhookDTO webhook
    ) {
        if (!"messages.upsert".equalsIgnoreCase(
                webhook.event()
        )) {
            return Optional.empty();
        }

        JsonNode data = webhook.data();
        JsonNode key = data.path("key");

        boolean fromMe = key
                .path("fromMe")
                .asBoolean(false);

        String remoteJid = key
                .path("remoteJid")
                .asText("");

        if (remoteJid.isBlank()
                || !remoteJid.endsWith("@s.whatsapp.net")) {
            return Optional.empty();
        }

        JsonNode messageNode = data.path("message");

        IncomingMessageType type =
                identifyMessageType(data, messageNode);

        String text = extractContent(
                messageNode,
                type
        );

        String phone = remoteJid.replace(
                "@s.whatsapp.net",
                ""
        );

        String pushName = data
                .path("pushName")
                .asText("");

        String messageId = key
                .path("id")
                .asText("");

        long timestamp = data
                .path("messageTimestamp")
                .asLong(0);

        Instant sentAt = timestamp > 0
                ? Instant.ofEpochSecond(timestamp)
                : null;

        return Optional.of(
                new IncomingMessageDTO(
                        webhook.instance(),
                        phone,
                        pushName,
                        text,
                        messageId,
                        sentAt,
                        type,
                        fromMe
                )
        );
    }

    private IncomingMessageType identifyMessageType(
            JsonNode data,
            JsonNode message
    ) {
        String messageType = data
                .path("messageType")
                .asText("");

        return switch (messageType) {
            case "conversation",
                 "extendedTextMessage" ->
                    IncomingMessageType.TEXT;

            case "imageMessage" ->
                    IncomingMessageType.IMAGE;

            case "audioMessage" ->
                    IncomingMessageType.AUDIO;

            case "videoMessage" ->
                    IncomingMessageType.VIDEO;

            case "documentMessage",
                 "documentWithCaptionMessage" ->
                    IncomingMessageType.DOCUMENT;

            case "locationMessage",
                 "liveLocationMessage" ->
                    IncomingMessageType.LOCATION;

            case "contactMessage",
                 "contactsArrayMessage" ->
                    IncomingMessageType.CONTACT;

            case "stickerMessage" ->
                    IncomingMessageType.STICKER;

            default -> identifyTypeFromMessageNode(message);
        };
    }

    private IncomingMessageType identifyTypeFromMessageNode(
            JsonNode message
    ) {
        if (message.has("conversation")
                || message.has("extendedTextMessage")) {
            return IncomingMessageType.TEXT;
        }

        if (message.has("imageMessage")) {
            return IncomingMessageType.IMAGE;
        }

        if (message.has("audioMessage")) {
            return IncomingMessageType.AUDIO;
        }

        if (message.has("videoMessage")) {
            return IncomingMessageType.VIDEO;
        }

        if (message.has("documentMessage")
                || message.has("documentWithCaptionMessage")) {
            return IncomingMessageType.DOCUMENT;
        }

        if (message.has("locationMessage")
                || message.has("liveLocationMessage")) {
            return IncomingMessageType.LOCATION;
        }

        if (message.has("contactMessage")
                || message.has("contactsArrayMessage")) {
            return IncomingMessageType.CONTACT;
        }

        if (message.has("stickerMessage")) {
            return IncomingMessageType.STICKER;
        }

        return IncomingMessageType.UNKNOWN;
    }

    private String extractContent(
            JsonNode message,
            IncomingMessageType type
    ) {
        return switch (type) {
            case TEXT -> extractText(message);

            case IMAGE -> extractImageCaption(message);

            case VIDEO -> extractVideoCaption(message);

            case DOCUMENT -> extractDocumentName(message);

            case AUDIO -> "[Áudio recebido]";

            case LOCATION -> "[Localização recebida]";

            case CONTACT -> "[Contato recebido]";

            case STICKER -> "[Figurinha recebida]";

            case UNKNOWN -> "[Tipo de mensagem não identificado]";
        };
    }

    private String extractText(JsonNode message) {
        if (message.hasNonNull("conversation")) {
            return message
                    .path("conversation")
                    .asText("");
        }

        if (message.has("extendedTextMessage")) {
            return message
                    .path("extendedTextMessage")
                    .path("text")
                    .asText("");
        }

        return "";
    }

    private String extractImageCaption(JsonNode message) {
        String caption = message
                .path("imageMessage")
                .path("caption")
                .asText("");

        if (caption.isBlank()) {
            return "[Imagem recebida]";
        }

        return "[Imagem recebida] " + caption.trim();
    }

    private String extractVideoCaption(JsonNode message) {
        String caption = message
                .path("videoMessage")
                .path("caption")
                .asText("");

        if (caption.isBlank()) {
            return "[Vídeo recebido]";
        }

        return "[Vídeo recebido] " + caption.trim();
    }

    private String extractDocumentName(JsonNode message) {
        JsonNode document = message.path("documentMessage");

        if (document.isMissingNode()) {
            document = message
                    .path("documentWithCaptionMessage")
                    .path("message")
                    .path("documentMessage");
        }

        String fileName = document
                .path("fileName")
                .asText("");

        if (fileName.isBlank()) {
            return "[Documento recebido]";
        }

        return "[Documento recebido: "
                + fileName.trim()
                + "]";
    }


}