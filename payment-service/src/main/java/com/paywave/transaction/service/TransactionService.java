package com.paywave.transaction.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.money.Money;
import com.paywave.transaction.domain.TransactionRecord;
import com.paywave.transaction.repo.TransactionRepository;
import com.paywave.transaction.web.TransactionRequest;
import com.paywave.transaction.web.TransactionResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactions;

    public TransactionService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Transactional
    public List<TransactionResponse> recordAll(List<TransactionRequest> requests) {
        List<TransactionResponse> saved = new ArrayList<>();
        for (TransactionRequest request : requests) {
            saved.add(record(request));
        }
        return saved;
    }

    @Transactional
    public TransactionResponse record(TransactionRequest request) {
        return transactions.findByPaymentIdAndOwnerIdAndDirectionAndReferenceType(
                        request.paymentId(), request.ownerId(), request.direction(), request.referenceType())
                .map(TransactionResponse::from)
                .orElseGet(() -> TransactionResponse.from(transactions.save(TransactionRecord.create(
                        request.paymentId(), request.ownerId(), request.counterpartyId(), request.counterpartyName(),
                        request.direction(), request.type(), Money.normalize(request.amount()), request.status(),
                        request.description(), request.referenceType()))));
    }

    @Transactional(readOnly = true)
    public Page<TransactionRecord> search(UUID ownerId, String type, String status, String direction, Instant from, Instant to, String query, Pageable pageable) {
        Specification<TransactionRecord> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));
            if (type != null && !type.isBlank()) predicates.add(cb.equal(root.get("type"), type));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), status));
            if (direction != null && !direction.isBlank()) predicates.add(cb.equal(root.get("direction"), direction));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            if (query != null && !query.isBlank()) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("counterpartyName"), "")), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return transactions.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID ownerId, UUID id) {
        TransactionRecord record = transactions.findById(id)
                .orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Transaction not found"));
        if (!record.getOwnerId().equals(ownerId)) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "You cannot view this transaction");
        }
        return TransactionResponse.from(record);
    }
}
