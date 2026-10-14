CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    template_id VARCHAR(255) NOT NULL,
    payload TEXT,
    status VARCHAR(50) NOT NULL,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    idempotency_key VARCHAR(255),
    sent_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    CONSTRAINT uk_notifications_user_idempotency UNIQUE (user_id, idempotency_key)
);

-- Indexes for common queries
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);