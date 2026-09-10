package com.example.event_service.repository;

import com.example.event_service.model.EventPackage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventPackageRepository extends JpaRepository<EventPackage,Long> {
    List<EventPackage> findAllByEvent_EventId(Long eventId);

    /**
     * Loads a package with a write lock (SELECT ... FOR UPDATE) so that concurrent
     * purchases on the same package are serialized until the transaction commits.
     * Without it, two requests can both read the same sold-seat count and oversell.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from EventPackage p where p.eventPackageId = :packageId")
    Optional<EventPackage> findByIdForUpdate(@Param("packageId") Long packageId);
}
