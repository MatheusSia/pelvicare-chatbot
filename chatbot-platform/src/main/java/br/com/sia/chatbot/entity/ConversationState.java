package br.com.sia.chatbot.entity;

public enum ConversationState {
    AWAITING_LGPD,
    MAIN_MENU,
    AWAITING_NAME,
    AWAITING_HANDOFF_REASON,
    HUMAN_HANDOFF
}