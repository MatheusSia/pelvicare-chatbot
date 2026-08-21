package br.com.sia.chatbot.integration.evolution;

import br.com.sia.chatbot.dto.SendTextRequest;
import br.com.sia.chatbot.exception.EvolutionCommunicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import br.com.sia.chatbot.dto.IncomingMessageDTO;
import br.com.sia.chatbot.integration.evolution.dto.MarkChatUnreadRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EvolutionClient {

    private final RestClient restClient;

    private static final Logger log =
            LoggerFactory.getLogger(EvolutionClient.class);

    public EvolutionClient(
            @Value("${evolution.url}") String evolutionUrl,
            @Value("${evolution.api-key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(evolutionUrl)
                .defaultHeader("apikey", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    public void sendText(
            String instance,
            String phone,
            String text
    ) {
        SendTextRequest request =
                new SendTextRequest(phone, text);

        try {
            restClient.post()
                    .uri(
                            "/message/sendText/{instance}",
                            instance
                    )
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception exception) {
            throw new EvolutionCommunicationException(
                    "Não foi possível enviar a mensagem para o telefone "
                            + phone
                            + " na instância "
                            + instance,
                    exception
            );
        }
    }

    public void markChatAsUnread(
            String instance,
            IncomingMessageDTO message
    ) {
        String remoteJid =
                message.phone() + "@s.whatsapp.net";

        Map<String, Object> key = Map.of(
                "remoteJid", remoteJid,
                "fromMe", false,
                "id", message.messageId(),
                "participant", remoteJid
        );

        Map<String, Object> lastMessage = Map.of(
                "key", key,
                "message", Map.of(
                        "conversation", message.text()
                )
        );

        MarkChatUnreadRequest request =
                new MarkChatUnreadRequest(
                        lastMessage,
                        remoteJid
                );

        try {
            restClient.post()
                    .uri(
                            "/chat/markChatUnread/{instance}",
                            instance
                    )
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception exception) {
            log.error(
                    "Não foi possível marcar a conversa como não lida | instância={} | telefone={} | messageId={}",
                    instance,
                    message.phone(),
                    message.messageId(),
                    exception
            );
        }
    }
}