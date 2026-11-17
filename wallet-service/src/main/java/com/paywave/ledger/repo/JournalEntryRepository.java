package com.paywave.ledger.repo;

import com.paywave.ledger.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey);
    List<JournalEntry> findByReferenceTypeAndReferenceIdOrderByPostedAtAsc(String referenceType, UUID referenceId);
}
