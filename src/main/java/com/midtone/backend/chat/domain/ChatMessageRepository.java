package com.midtone.backend.chat.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByUserIdOrderByCreatedAtDescIdDesc(long userId, Pageable pageable);

    List<ChatMessage> findTop10ByUserIdOrderByCreatedAtDescIdDesc(long userId);

    Optional<ChatMessage> findByIdAndUserId(long id, long userId);
}
