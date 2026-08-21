package br.com.sia.chatbot.webhook;

import br.com.sia.chatbot.dto.EvolutionWebhookDTO;
import br.com.sia.chatbot.service.ChatbotService;
import br.com.sia.chatbot.service.EvolutionMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class EvolutionWebhookController {

    private static final Logger log =
            LoggerFactory.getLogger(EvolutionWebhookController.class);

    private final EvolutionMessageParser messageParser;
    private final ChatbotService chatbotService;

    public EvolutionWebhookController(
            EvolutionMessageParser messageParser,
            ChatbotService chatbotService
    ) {
        this.messageParser = messageParser;
        this.chatbotService = chatbotService;
    }

    @PostMapping("/evolution")
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody EvolutionWebhookDTO webhook
    ) {
        try {
            messageParser.parse(webhook).ifPresent(message -> {
                log.info(
                        "Mensagem recebida | instância={} | telefone={} | nome={} | tipo={} | texto={}",
                        message.instance(),
                        message.phone(),
                        message.pushName(),
                        message.type(),
                        message.text()
                );

                chatbotService.process(message);
            });

        } catch (Exception exception) {
            log.error(
                    "Erro ao processar webhook da Evolution | evento={} | instância={}",
                    webhook.event(),
                    webhook.instance(),
                    exception
            );
        }

        return ResponseEntity.ok().build();
    }
}