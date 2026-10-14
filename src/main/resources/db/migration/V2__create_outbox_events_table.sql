CREATE TABLE outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_payload TEXT NOT NULL,
    publication_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    last_retry_at TIMESTAMP,
    next_retry_at TIMESTAMP,
    CONSTRAINT uk_outbox_events_event_id UNIQUE (event_id)
);

-- Indexes for efficient polling
CREATE INDEX idx_outbox_events_publication_status ON outbox_events(publication_status);
CREATE INDEX idx_outbox_events_next_retry_at ON outbox_events(next_retry_at) WHERE next_retry_at IS NOT NULL;
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);