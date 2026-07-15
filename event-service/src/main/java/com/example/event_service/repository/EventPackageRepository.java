package com.example.event_service.repository;

import com.example.event_service.model.EventPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventPackageRepository extends JpaRepository<EventPackage,Long> {
    List<EventPackage> findAllByEvent_EventId(Long eventId);
}
