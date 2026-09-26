package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Newest first; the caller reverses for display. */
    @Query("SELECT m FROM Message m WHERE (m.sender.id = :a AND m.recipient.id = :b) "
            + "OR (m.sender.id = :b AND m.recipient.id = :a) ORDER BY m.createdAt DESC, m.id DESC")
    List<Message> findThread(@Param("a") Long a, @Param("b") Long b, Pageable page);

    /** Every message this user sent or received, newest first, for the conversation list. */
    @Query("SELECT m FROM Message m WHERE m.sender.id = :userId OR m.recipient.id = :userId "
            + "ORDER BY m.createdAt DESC, m.id DESC")
    List<Message> findAllInvolving(@Param("userId") Long userId, Pageable page);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Query("SELECT m.sender.id, COUNT(m) FROM Message m WHERE m.recipient.id = :userId AND m.readAt IS NULL GROUP BY m.sender.id")
    List<Object[]> unreadBySender(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :now WHERE m.recipient.id = :me AND m.sender.id = :other AND m.readAt IS NULL")
    int markRead(@Param("me") Long me, @Param("other") Long other, @Param("now") LocalDateTime now);
}
