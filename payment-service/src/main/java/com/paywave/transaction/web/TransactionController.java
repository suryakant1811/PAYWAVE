package com.paywave.transaction.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.api.PageResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.transaction.service.TransactionService;
import com.paywave.web.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
public class TransactionController {

    private static final Set<String> SORTS = Set.of("occurredAt", "amount", "status");
    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Search the authenticated customer's transaction history")
    @GetMapping("/api/v1/transactions")
    public ApiResponse<PageResponse<TransactionResponse>> search(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {
        if (!SORTS.contains(sort)) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "sort must be occurredAt, amount, or status");
        }
        Sort.Direction directionSort = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result = transactions.search(SecurityUtils.currentUserId(), type, status, direction, from, to, query,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(directionSort, sort)));
        return ApiResponse.ok(new PageResponse<>(result.map(TransactionResponse::from).getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/api/v1/transactions/{id}")
    public ApiResponse<TransactionResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(transactions.get(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/internal/transactions")
    public ApiResponse<PageResponse<TransactionResponse>> internalSearch(@RequestParam UUID ownerId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        var result = transactions.search(ownerId, null, null, null, null, null, null,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "occurredAt")));
        return ApiResponse.ok(new PageResponse<>(result.map(TransactionResponse::from).getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @PostMapping("/internal/transactions/batch")
    public ApiResponse<List<TransactionResponse>> batch(@RequestBody List<TransactionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "At least one transaction is required");
        }
        return ApiResponse.ok(transactions.recordAll(requests));
    }
}
