package com.fogui.backend.repository;

import com.fogui.backend.entity.ChatSession;
import com.fogui.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /**
     * Get all chat sessions for a user, ordered by most recent first.
     */
    List<ChatSession> findByUserOrderByUpdatedAtDesc(User user);

    /**
     * Get the most recent N sessions for a user.
     */
    List<ChatSession> findTop10ByUserOrderByUpdatedAtDesc(User user);
}
