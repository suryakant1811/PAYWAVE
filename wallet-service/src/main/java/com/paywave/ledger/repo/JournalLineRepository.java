package com.paywave.ledger.repo;

import com.paywave.ledger.domain.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {
    List<JournalLine> findByJournalEntryId(UUID journalEntryId);

    @Query(value = """
            SELECT
              COALESCE(SUM(CASE WHEN direction = 'DEBIT' THEN amount ELSE 0 END), 0),
              COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE 0 END), 0)
            FROM journal_lines
            WHERE account_id = :accountId
            """, nativeQuery = true)
    List<Object[]> sumSides(@Param("accountId") String accountId);
}
