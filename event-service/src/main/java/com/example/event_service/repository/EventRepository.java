package com.example.event_service.repository;

import com.example.event_service.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event,Long> {
    boolean existsByEventIdAndOwnerUserId(Long eventId, String ownerUserId);
    Page<Event> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
