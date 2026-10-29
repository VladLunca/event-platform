package com.example.event_service.repository;

import com.example.event_service.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,String> {
    List<Ticket> findByEventPackage_EventPackageId(Long packageId);
    long countByEventPackage_EventPackageId(Long packageId);
}
