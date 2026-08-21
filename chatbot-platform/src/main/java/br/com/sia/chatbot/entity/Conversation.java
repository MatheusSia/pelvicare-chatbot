package br.com.sia.chatbot.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversation_instance_phone",
                        columnNames = {"instance", "phone"}
                )
        }
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String instance;

    @Column(nullable = false)
    private String phone;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationState state;

    @Column(name = "handoff_reason", columnDefinition = "TEXT")
    private String handoffReason;

    private LocalDateTime lgpdAcceptedAt;

    private LocalDateTime humanHandoffAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConversationState getState() {
        return state;
    }

    public void setState(ConversationState state) {
        this.state = state;
    }

    public LocalDateTime getLgpdAcceptedAt() {
        return lgpdAcceptedAt;
    }

    public void setLgpdAcceptedAt(LocalDateTime lgpdAcceptedAt) {
        this.lgpdAcceptedAt = lgpdAcceptedAt;
    }

    public LocalDateTime getHumanHandoffAt() {
        return humanHandoffAt;
    }

    public void setHumanHandoffAt(LocalDateTime humanHandoffAt) {
        this.humanHandoffAt = humanHandoffAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getHandoffReason() {
        return handoffReason;
    }

    public void setHandoffReason(String handoffReason) {
        this.handoffReason = handoffReason;
    }
}