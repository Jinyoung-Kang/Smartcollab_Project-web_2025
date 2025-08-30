package com.smartcollab.prod.repository;

import com.smartcollab.prod.entity.ChatMessage;
import com.smartcollab.prod.entity.Team;
import com.smartcollab.prod.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByTeamOrderByCreatedAtAsc(Team team);
    void deleteByTeam(Team team);
    List<ChatMessage> findBySender(User sender);
}