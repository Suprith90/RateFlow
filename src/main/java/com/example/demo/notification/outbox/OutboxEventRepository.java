package com.example.demo.notification.outbox;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for OutboxEvent entities.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Find pending outbox events that are ready for processing.
     * @param limit maximum number of events to return
     * @return list of pending events
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.publicationStatus = 'PENDING' " +
           "AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now) " +
           "ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEventsReadyForProcessing(@Param("now") Instant now,
                                                          @Param("limit") int limit);

    /**
     * Find an outbox event by its unique event ID.
     * @param eventId the unique event identifier
     * @return the outbox event if found
     */
    Optional<OutboxEvent> findByEventId(String eventId);

    /**
     * Count of outbox events by publication status.
     * @param status the publication status to count
     * @return number of events with the given status
     */
    long countByPublicationStatus(OutboxEvent.PublicationStatus status);

    /**
     * Find failed outbox events that have exceeded retry limits.
     * @param limit maximum number of events to return
     * @return list of failed events
     */
    List<OutboxEvent> findByPublicationStatusOrderByCreatedAtDesc(OutboxEvent.PublicationStatus status);

    /**
     * Find outbox events that are ready for retry (pending with future retry time).
     * @param limit maximum number of events to return
     * @return list of events ready for retry
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.publicationStatus = 'PENDING' AND o.nextRetryAt IS NOT NULL AND o.nextRetryAt > :now " +
           "ORDER BY o.nextRetryAt ASC")
    List<OutboxEvent> findByPublicationStatusAndNextRetryAtGreaterThanEqualOrderByNextRetryAtAsc(
            OutboxEvent.PublicationStatus status, Instant now);

    /**
     * Count of pending outbox events.
     * @return number of pending events
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.publicationStatus = 'PENDING'")
    long countPendingEvents();

    /**
     * Count of failed outbox events.
     * @return number of failed events
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.publicationStatus = 'FAILED'")
    long countFailedEvents();

    /**
     * Average retry count for outbox events.
     * @return average retry count
     */
    @Query("SELECT AVG(o.retryCount) FROM OutboxEvent o")
    double averageRetryCount();
}