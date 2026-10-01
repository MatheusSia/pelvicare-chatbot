package br.com.sia.chatbot.service;

import br.com.sia.chatbot.config.ApplicationStartupTime;
import br.com.sia.chatbot.dto.IncomingMessageDTO;
import br.com.sia.chatbot.dto.IncomingMessageType;
import br.com.sia.chatbot.entity.Conversation;
import br.com.sia.chatbot.entity.ConversationState;
import br.com.sia.chatbot.exception.EvolutionCommunicationException;
import br.com.sia.chatbot.integration.evolution.EvolutionClient;
import br.com.sia.chatbot.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import br.com.sia.chatbot.entity.ProcessedMessage;
import br.com.sia.chatbot.repository.ProcessedMessageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import br.com.sia.chatbot.flow.MessageCode;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ChatbotService {

    private static final Logger log =
            LoggerFactory.getLogger(ChatbotService.class);

    private final ApplicationStartupTime applicationStartupTime;
    private final EvolutionClient evolutionClient;
    private final ConversationRepository conversationRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ChatbotMessageService chatbotMessageService;
    private final ChatMessageService chatMessageService;
    private final MenuOptionResolver menuOptionResolver;

    public ChatbotService(
            EvolutionClient evolutionClient,
            ConversationRepository conversationRepository,
            ApplicationStartupTime applicationStartupTime,
            ProcessedMessageRepository processedMessageRepository,
            ChatbotMessageService chatbotMessageService,
            ChatMessageService chatMessageService,
            MenuOptionResolver menuOptionResolver
    ) {
        this.evolutionClient = evolutionClient;
        this.conversationRepository = conversationRepository;
        this.applicationStartupTime = applicationStartupTime;
        this.processedMessageRepository = processedMessageRepository;
        this.chatbotMessageService = chatbotMessageService;
        this.chatMessageService = chatMessageService;
        this.menuOptionResolver = menuOptionResolver;
    }

    public void process(IncomingMessageDTO message) {
        boolean messageRegistered = false;

        try {
            if (isMessageOlderThanApplication(message)) {
                log.info(
                        "Mensagem antiga ignorada | telefone={} | mensagem={} | enviadaEm={} | aplicacaoIniciadaEm={}",
                        message.phone(),
                        message.messageId(),
                        message.sentAt(),
                        applicationStartupTime.getStartedAt()
                );

                return;
            }

            if (!registerMessageIfNew(message)) {
                log.info(
                        "Mensagem duplicada ignorada | instância={} | messageId={}",
                        message.instance(),
                        message.messageId()
                );

                return;
            }

            messageRegistered = true;

            processInternal(message);

        } catch (Exception exception) {
            if (messageRegistered) {
                removeProcessedMessage(message);
            }

            log.error(
                    "Erro ao processar mensagem | instância={} | telefone={} | messageId={} | tipo={}",
                    message.instance(),
                    message.phone(),
                    message.messageId(),
                    message.type(),
                    exception
            );

            throw exception;
        }
    }

    private void removeProcessedMessage(
            IncomingMessageDTO message
    ) {
        try {
            processedMessageRepository
                    .deleteByInstanceAndMessageId(
                            message.instance(),
                            message.messageId()
                    );

            log.info(
                    "Registro de processamento removido após erro | instância={} | messageId={}",
                    message.instance(),
                    message.messageId()
            );

        } catch (Exception cleanupException) {
            log.error(
                    "Erro ao remover registro de processamento | instância={} | messageId={}",
                    message.instance(),
                    message.messageId(),
                    cleanupException
            );
        }
    }

    private void processInternal(
            IncomingMessageDTO message
    ) {
        log.info(
                "Processando mensagem | telefone={} | tipo={} | conteúdo={}",
                message.phone(),
                message.type(),
                message.text()
        );

        var conversationOptional = conversationRepository
                .findByInstanceAndPhone(
                        message.instance(),
                        message.phone()
                );

        if (conversationOptional.isEmpty()) {
            if (message.fromMe()) {
                Conversation conversation =
                        createSilencedConversation(message);

                chatMessageService.saveClinicOutbound(
                        conversation,
                        message
                );

                log.info(
                        "Conversa silenciada porque a clínica já estava falando | telefone={}",
                        message.phone()
                );

                return;
            }

            Conversation conversation =
                    createConversationWithoutSending(message);

            chatMessageService.saveInbound(
                    conversation,
                    message
            );

            send(
                    conversation,
                    message,
                    getMessage(
                            message,
                            MessageCode.LGPD
                    )
            );

            return;
        }

        Conversation conversation =
                conversationOptional.get();

        if (message.fromMe()) {
            processClinicMessage(
                    conversation,
                    message
            );

            return;
        }

        chatMessageService.saveInbound(
                conversation,
                message
        );

        if (conversation.getState()
                == ConversationState.HUMAN_HANDOFF) {

            processHumanHandoff(
                    conversation,
                    message
            );

            return;
        }

        if (conversation.getState()
                == ConversationState.AWAITING_LGPD) {

            processLgpd(
                    conversation,
                    message
            );

            return;
        }

        if (message.type()
                != IncomingMessageType.TEXT) {

            processNonTextMessage(
                    conversation,
                    message
            );

            return;
        }

        switch (conversation.getState()) {
            case MAIN_MENU ->
                    processMainMenu(
                            conversation,
                            message
                    );

            case AWAITING_NAME ->
                    processName(
                            conversation,
                            message
                    );

            case AWAITING_HANDOFF_REASON ->
                    processHandoffReason(
                            conversation,
                            message
                    );

            case AWAITING_LGPD,
                 HUMAN_HANDOFF -> {
            }
        }
    }

    private void processNonTextMessage(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        switch (message.type()) {
            case AUDIO ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.UNSUPPORTED_AUDIO
                            )
                    );

            case IMAGE,
                 VIDEO,
                 DOCUMENT,
                 LOCATION,
                 CONTACT -> {
                conversation.setState(
                        ConversationState.AWAITING_NAME
                );

                conversationRepository.save(conversation);

                send(
                        conversation,
                        message,
                        getMessage(
                                message,
                                MessageCode.MEDIA_RECEIVED
                        )
                );
            }

            case STICKER ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.UNSUPPORTED_STICKER
                            )
                    );

            case UNKNOWN ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.UNKNOWN_MESSAGE_TYPE
                            )
                    );

            case TEXT -> {
                // Mensagens de texto não chegam neste método.
            }
        }
    }

    private Conversation createConversationWithoutSending(
            IncomingMessageDTO message
    ) {
        Conversation conversation = new Conversation();
        conversation.setInstance(message.instance());
        conversation.setPhone(message.phone());
        conversation.setName(message.pushName());
        conversation.setState(ConversationState.AWAITING_LGPD);

        return conversationRepository.save(conversation);
    }

    private Conversation createSilencedConversation(
            IncomingMessageDTO message
    ) {
        Conversation conversation = new Conversation();
        conversation.setInstance(message.instance());
        conversation.setPhone(message.phone());
        conversation.setName(message.pushName());
        conversation.setState(ConversationState.HUMAN_HANDOFF);
        conversation.setHumanHandoffAt(LocalDateTime.now());
        conversation.setHandoffReason(
                "Atendimento já em andamento pela clínica"
        );

        return conversationRepository.save(conversation);
    }

    private void processClinicMessage(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        if (isBotEcho(message)) {
            log.debug(
                    "Eco da mensagem do bot ignorado | telefone={} | messageId={}",
                    message.phone(),
                    message.messageId()
            );

            return;
        }

        chatMessageService.saveClinicOutbound(
                conversation,
                message
        );

        if (conversation.getState()
                == ConversationState.HUMAN_HANDOFF) {
            return;
        }

        silenceConversation(
                conversation,
                "Intervenção da clínica no WhatsApp"
        );

        log.info(
                "Bot encerrado porque a clínica enviou mensagem nesta conversa | telefone={}",
                message.phone()
        );
    }

    private boolean isBotEcho(IncomingMessageDTO message) {
        return chatbotMessageService.matchesStoredContent(
                message.instance(),
                message.text()
        );
    }

    private void silenceConversation(
            Conversation conversation,
            String reason
    ) {
        conversation.setState(ConversationState.HUMAN_HANDOFF);
        conversation.setHumanHandoffAt(LocalDateTime.now());
        conversation.setHandoffReason(reason);

        conversationRepository.save(conversation);
    }

    private boolean registerMessageIfNew(
            IncomingMessageDTO message
    ) {
        if (message.messageId() == null
                || message.messageId().isBlank()) {

            log.warn(
                    "Mensagem sem messageId ignorada | telefone={}",
                    message.phone()
            );

            return false;
        }

        if (processedMessageRepository
                .existsByInstanceAndMessageId(
                        message.instance(),
                        message.messageId()
                )) {

            return false;
        }

        ProcessedMessage processedMessage =
                new ProcessedMessage();

        processedMessage.setInstance(message.instance());
        processedMessage.setMessageId(message.messageId());
        processedMessage.setProcessedAt(Instant.now());

        try {
            processedMessageRepository.saveAndFlush(
                    processedMessage
            );

            return true;

        } catch (DataIntegrityViolationException exception) {
            /*
             * Protege contra dois webhooks iguais chegando
             * praticamente ao mesmo tempo.
             */
            return false;
        }
    }

    private void registerProcessedMessageId(
            String instance,
            String messageId
    ) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }

        if (processedMessageRepository.existsByInstanceAndMessageId(
                instance,
                messageId
        )) {
            return;
        }

        ProcessedMessage processedMessage =
                new ProcessedMessage();

        processedMessage.setInstance(instance);
        processedMessage.setMessageId(messageId);
        processedMessage.setProcessedAt(Instant.now());

        try {
            processedMessageRepository.saveAndFlush(
                    processedMessage
            );
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    private String getMessage(
            IncomingMessageDTO incomingMessage,
            String code
    ) {
        return chatbotMessageService.getMessage(
                incomingMessage.instance(),
                code
        );
    }

    private void processHumanHandoff(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        log.info(
                "Mensagem ignorada pelo bot porque a conversa está em atendimento humano permanente | telefone={} | nome={} | assunto={}",
                message.phone(),
                conversation.getName(),
                conversation.getHandoffReason()
        );
    }

    private void processLgpd(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        String text = normalize(message.text());

        if (text.equals("1")) {

            conversation.setLgpdAcceptedAt(LocalDateTime.now());
            conversation.setState(ConversationState.MAIN_MENU);

            conversationRepository.save(conversation);

            send(
                    conversation,
                    message,
                    getMessage(
                            message,
                            MessageCode.MAIN_MENU
                    )
            );

            return;
        }

        if (text.equals("2")) {
            silenceConversation(
                    conversation,
                    "Cliente recusou o atendimento automático"
            );

            send(
                    conversation,
                    message,
                    getMessage(
                            message,
                            MessageCode.BOT_STOPPED
                    )
            );

            return;
        }

        send(
                conversation,
                message,
                getMessage(
                        message,
                        MessageCode.LGPD_RETRY
                )
        );
    }

    private void processMainMenu(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        String option =
                menuOptionResolver.identify(message.text());

        switch (option) {
            case "1" ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.TREATMENTS
                            )
                    );

            case "2" ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.HEALTH_PLANS
                            )
                    );

            case "3" ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.UNIMED_GUIDE
                            )
                    );

            case "4" ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.SCHEDULE
                            )
                    );

            case "5" ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.ADDRESS
                            )
                    );

            case "6" -> {
                conversation.setState(
                        ConversationState.AWAITING_NAME
                );

                conversationRepository.save(conversation);

                send(
                        conversation,
                        message,
                        getMessage(
                                message,
                                MessageCode.REQUEST_NAME
                        )
                );
            }

            case "7" ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.REFERRALS
                            )
                    );

            case "menu" ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.MAIN_MENU
                            )
                    );

            default ->
                    send(
                            conversation,
                            message,
                            getMessage(
                                    message,
                                    MessageCode.INVALID_OPTION
                            )
                    );
        }
    }

    private void processName(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        String name = message.text().trim();

        if (!isValidName(name)) {
            send(
                    conversation,
                    message,
                    getMessage(
                            message,
                            MessageCode.INVALID_NAME
                    )
            );

            return;
        }

        conversation.setName(name);
        conversation.setState(
                ConversationState.AWAITING_HANDOFF_REASON
        );

        conversationRepository.save(conversation);

        String requestReasonMessage = getMessage(
                message,
                MessageCode.REQUEST_HANDOFF_REASON
        ).replace(
                "{{name}}",
                conversation.getName()
        );

        send(
                conversation,
                message,
                requestReasonMessage
        );
    }

    private boolean isValidName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        String trimmedName = name.trim();

        if (trimmedName.length() < 3
                || trimmedName.length() > 100) {
            return false;
        }

        return trimmedName.matches(
                "[\\p{L}]+(?:[ '-][\\p{L}]+)*"
        );
    }

    private void processHandoffReason(
            Conversation conversation,
            IncomingMessageDTO message
    ) {
        String reason = message.text().trim();

        if (!isValidHandoffReason(reason)) {
            send(
                    conversation,
                    message,
                    getMessage(
                            message,
                            MessageCode.INVALID_HANDOFF_REASON
                    )
            );

            return;
        }

        conversation.setHandoffReason(reason);
        conversation.setState(ConversationState.HUMAN_HANDOFF);
        conversation.setHumanHandoffAt(LocalDateTime.now());

        conversationRepository.save(conversation);

        String confirmationMessage = getMessage(
                message,
                MessageCode.HANDOFF_CONFIRMATION
        )
                .replace(
                        "{{name}}",
                        conversation.getName()
                )
                .replace(
                        "{{reason}}",
                        conversation.getHandoffReason()
                );

        send(
                conversation,
                message,
                confirmationMessage
        );

        evolutionClient.markChatAsUnread(
                message.instance(),
                message
        );

        log.info(
                "Atendimento encaminhado para secretária e conversa marcada como não lida | telefone={} | nome={} | assunto={}",
                message.phone(),
                conversation.getName(),
                conversation.getHandoffReason()
        );
    }

    private boolean isValidHandoffReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return false;
        }

        String trimmedReason = reason.trim();

        return trimmedReason.length() >= 5
                && trimmedReason.length() <= 500;
    }

    private void send(
            Conversation conversation,
            IncomingMessageDTO message,
            String text
    ) {
        try {
            Optional<String> outboundMessageId =
                    evolutionClient.sendText(
                            message.instance(),
                            message.phone(),
                            text
                    );

            if (outboundMessageId != null) {
                outboundMessageId.ifPresent(
                        id -> registerProcessedMessageId(
                                message.instance(),
                                id
                        )
                );
            }

            chatMessageService.saveOutbound(
                    conversation,
                    message.instance(),
                    text
            );

            log.info(
                    "Mensagem enviada com sucesso | telefone={} | conversa={}",
                    message.phone(),
                    conversation.getId()
            );

        } catch (EvolutionCommunicationException exception) {
            log.error(
                    "Erro ao enviar mensagem pela Evolution | telefone={} | conversa={}",
                    message.phone(),
                    conversation.getId(),
                    exception
            );

            throw exception;
        }
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(
                value.trim().toLowerCase(),
                Normalizer.Form.NFD
        );

        return normalized.replaceAll("\\p{M}", "");
    }

    private boolean isMessageOlderThanApplication(
            IncomingMessageDTO message
    ) {
        if (message.sentAt() == null) {
            log.warn(
                    "Mensagem ignorada porque não possui timestamp | id={}",
                    message.messageId()
            );

            return true;
        }

        Instant cutoffTime = applicationStartupTime
                .getStartedAt()
                .minusSeconds(30);

        return message.sentAt().isBefore(cutoffTime);
    }

    private boolean containsAny(
            String text,
            String... terms
    ) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }

        return false;
    }

}
