package br.com.sia.chatbot.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "processed_messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_message_instance_id",
                        columnNames = {"instance", "message_id"}
                )
        }
)
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String instance;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(nullable = false)
    private Instant processedAt;

    public Long getId() {
        return id;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}