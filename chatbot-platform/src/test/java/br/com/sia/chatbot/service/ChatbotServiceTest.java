package br.com.sia.chatbot.service;

import br.com.sia.chatbot.config.ApplicationStartupTime;
import br.com.sia.chatbot.dto.IncomingMessageDTO;
import br.com.sia.chatbot.dto.IncomingMessageType;
import br.com.sia.chatbot.entity.Conversation;
import br.com.sia.chatbot.entity.ConversationState;
import br.com.sia.chatbot.entity.ProcessedMessage;
import br.com.sia.chatbot.integration.evolution.EvolutionClient;
import br.com.sia.chatbot.repository.ConversationRepository;
import br.com.sia.chatbot.repository.ProcessedMessageRepository;
import br.com.sia.chatbot.flow.MessageCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private EvolutionClient evolutionClient;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ApplicationStartupTime applicationStartupTime;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @Mock
    private ChatbotMessageService chatbotMessageService;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private MenuOptionResolver menuOptionResolver;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                evolutionClient,
                conversationRepository,
                applicationStartupTime,
                processedMessageRepository,
                chatbotMessageService,
                chatMessageService,
                menuOptionResolver
        );
    }

    @Test
    void shouldCreateConversationAndSendLgpdOnFirstContact() {
        IncomingMessageDTO message = createTextMessage(
                "MESSAGE-1",
                "Olá",
                Instant.parse("2026-07-28T14:10:00Z")
        );

        when(applicationStartupTime.getStartedAt())
                .thenReturn(
                        Instant.parse("2026-07-28T14:00:00Z")
                );

        when(
                processedMessageRepository
                        .existsByInstanceAndMessageId(
                                "pelvicare",
                                "MESSAGE-1"
                        )
        ).thenReturn(false);

        when(
                processedMessageRepository
                        .saveAndFlush(any(ProcessedMessage.class))
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        when(
                conversationRepository
                        .findByInstanceAndPhone(
                                "pelvicare",
                                "5519999999999"
                        )
        ).thenReturn(Optional.empty());

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(
                chatbotMessageService.getMessage(
                        "pelvicare",
                        MessageCode.LGPD
                )
        ).thenReturn("Mensagem da LGPD");

        chatbotService.process(message);

        verify(conversationRepository)
                .save(any(Conversation.class));

        verify(chatMessageService)
                .saveInbound(
                        any(Conversation.class),
                        eq(message)
                );

        verify(evolutionClient)
                .sendText(
                        "pelvicare",
                        "5519999999999",
                        "Mensagem da LGPD"
                );

        verify(chatMessageService)
                .saveOutbound(
                        any(Conversation.class),
                        eq("pelvicare"),
                        eq("Mensagem da LGPD")
                );
    }

    @Test
    void shouldIgnoreDuplicateMessage() {
        IncomingMessageDTO message = createTextMessage(
                "DUPLICATE-1",
                "Olá",
                Instant.parse("2026-07-28T14:10:00Z")
        );

        when(applicationStartupTime.getStartedAt())
                .thenReturn(
                        Instant.parse("2026-07-28T14:00:00Z")
                );

        when(
                processedMessageRepository
                        .existsByInstanceAndMessageId(
                                "pelvicare",
                                "DUPLICATE-1"
                        )
        ).thenReturn(true);

        chatbotService.process(message);

        verifyNoInteractions(conversationRepository);
        verifyNoInteractions(evolutionClient);
        verifyNoInteractions(chatMessageService);
    }

    @Test
    void shouldIgnoreOldMessage() {
        IncomingMessageDTO message = createTextMessage(
                "OLD-1",
                "Mensagem antiga",
                Instant.parse("2026-07-28T13:00:00Z")
        );

        when(applicationStartupTime.getStartedAt())
                .thenReturn(
                        Instant.parse("2026-07-28T14:00:00Z")
                );

        chatbotService.process(message);

        verifyNoInteractions(processedMessageRepository);
        verifyNoInteractions(conversationRepository);
        verifyNoInteractions(evolutionClient);
    }

    @Test
    void shouldRemoveProcessedRecordWhenProcessingFails() {
        IncomingMessageDTO message = createTextMessage(
                "FAIL-1",
                "Olá",
                Instant.parse("2026-07-28T14:10:00Z")
        );

        when(applicationStartupTime.getStartedAt())
                .thenReturn(
                        Instant.parse("2026-07-28T14:00:00Z")
                );

        when(
                processedMessageRepository
                        .existsByInstanceAndMessageId(
                                "pelvicare",
                                "FAIL-1"
                        )
        ).thenReturn(false);

        when(
                processedMessageRepository
                        .saveAndFlush(any(ProcessedMessage.class))
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        when(
                conversationRepository
                        .findByInstanceAndPhone(
                                anyString(),
                                anyString()
                        )
        ).thenThrow(
                new RuntimeException("Banco indisponível")
        );

        try {
            chatbotService.process(message);
        } catch (RuntimeException ignored) {
        }

        verify(processedMessageRepository)
                .deleteByInstanceAndMessageId(
                        "pelvicare",
                        "FAIL-1"
                );
    }

    @Test
    void shouldStopBotWhenClientChoosesOptionTwoOnLgpd() {
        IncomingMessageDTO message = createTextMessage(
                "LGPD-2",
                "2",
                Instant.parse("2026-07-28T14:10:00Z")
        );

        Conversation conversation = awaitingLgpdConversation();

        stubRecentNewMessage("LGPD-2");

        when(
                conversationRepository.findByInstanceAndPhone(
                        "pelvicare",
                        "5519999999999"
                )
        ).thenReturn(Optional.of(conversation));

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(
                chatbotMessageService.getMessage(
                        "pelvicare",
                        MessageCode.BOT_STOPPED
                )
        ).thenReturn("Bot encerrado");

        chatbotService.process(message);

        assertEquals(
                ConversationState.HUMAN_HANDOFF,
                conversation.getState()
        );

        verify(evolutionClient).sendText(
                "pelvicare",
                "5519999999999",
                "Bot encerrado"
        );
    }

    @Test
    void shouldSilenceChatWhenClinicWritesFirst() {
        IncomingMessageDTO message = createFromMeMessage(
                "CLINIC-1",
                "Oi, já estou te atendendo",
                Instant.parse("2026-07-28T14:10:00Z")
        );

        stubRecentNewMessage("CLINIC-1");

        when(
                conversationRepository.findByInstanceAndPhone(
                        "pelvicare",
                        "5519999999999"
                )
        ).thenReturn(Optional.empty());

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        chatbotService.process(message);

        ArgumentCaptor<Conversation> captor =
                ArgumentCaptor.forClass(Conversation.class);

        verify(conversationRepository).save(captor.capture());

        assertEquals(
                ConversationState.HUMAN_HANDOFF,
                captor.getValue().getState()
        );

        verify(chatMessageService).saveClinicOutbound(
                any(Conversation.class),
                eq(message)
        );

        verify(evolutionClient, never())
                .sendText(anyString(), anyString(), anyString());
    }

    @Test
    void shouldSilenceChatWhenClinicWritesDuringLgpd() {
        IncomingMessageDTO message = createFromMeMessage(
                "CLINIC-2",
                "2",
                Instant.parse("2026-07-28T14:10:00Z")
        );

        Conversation conversation = awaitingLgpdConversation();

        stubRecentNewMessage("CLINIC-2");

        when(
                conversationRepository.findByInstanceAndPhone(
                        "pelvicare",
                        "5519999999999"
                )
        ).thenReturn(Optional.of(conversation));

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(
                chatbotMessageService.matchesStoredContent(
                        "pelvicare",
                        "2"
                )
        ).thenReturn(false);

        chatbotService.process(message);

        assertEquals(
                ConversationState.HUMAN_HANDOFF,
                conversation.getState()
        );

        verify(chatMessageService).saveClinicOutbound(
                conversation,
                message
        );

        verify(evolutionClient, never())
                .sendText(anyString(), anyString(), anyString());
    }

    @Test
    void shouldIgnoreBotEchoFromClinicWhatsapp() {
        IncomingMessageDTO message = createFromMeMessage(
                "ECHO-1",
                "Mensagem da LGPD",
                Instant.parse("2026-07-28T14:10:00Z")
        );

        Conversation conversation = awaitingLgpdConversation();

        stubRecentNewMessage("ECHO-1");

        when(
                conversationRepository.findByInstanceAndPhone(
                        "pelvicare",
                        "5519999999999"
                )
        ).thenReturn(Optional.of(conversation));

        when(
                chatbotMessageService.matchesStoredContent(
                        "pelvicare",
                        "Mensagem da LGPD"
                )
        ).thenReturn(true);

        chatbotService.process(message);

        assertEquals(
                ConversationState.AWAITING_LGPD,
                conversation.getState()
        );

        verify(conversationRepository, never())
                .save(any(Conversation.class));

        verify(evolutionClient, never())
                .sendText(anyString(), anyString(), anyString());
    }

    private void stubRecentNewMessage(String messageId) {
        when(applicationStartupTime.getStartedAt())
                .thenReturn(
                        Instant.parse("2026-07-28T14:00:00Z")
                );

        when(
                processedMessageRepository.existsByInstanceAndMessageId(
                        "pelvicare",
                        messageId
                )
        ).thenReturn(false);

        when(
                processedMessageRepository.saveAndFlush(
                        any(ProcessedMessage.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );
    }

    private Conversation awaitingLgpdConversation() {
        Conversation conversation = new Conversation();
        conversation.setInstance("pelvicare");
        conversation.setPhone("5519999999999");
        conversation.setState(ConversationState.AWAITING_LGPD);
        return conversation;
    }

    private IncomingMessageDTO createTextMessage(
            String messageId,
            String text,
            Instant sentAt
    ) {
        return new IncomingMessageDTO(
                "pelvicare",
                "5519999999999",
                "Rodrigo Sia",
                text,
                messageId,
                sentAt,
                IncomingMessageType.TEXT,
                false
        );
    }

    private IncomingMessageDTO createFromMeMessage(
            String messageId,
            String text,
            Instant sentAt
    ) {
        return new IncomingMessageDTO(
                "pelvicare",
                "5519999999999",
                "PelviCare",
                text,
                messageId,
                sentAt,
                IncomingMessageType.TEXT,
                true
        );
    }
}