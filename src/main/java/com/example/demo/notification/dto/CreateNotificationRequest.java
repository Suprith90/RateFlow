package com.example.demo.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * Request DTO for creating a notification.
 * Contains all the information needed to create a new notification.
 */
public class CreateNotificationRequest implements Serializable {

    @NotNull
    private NotificationType type;

    @NotBlank
    @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Invalid email format")
    private String recipient;

    @NotBlank
    private String templateId;

    @NotBlank
    private String payload;

    private String idempotencyKey;

    // Getters and Setters
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    // Enums
    public enum NotificationType {
        EMAIL,
        SMS
    }
}