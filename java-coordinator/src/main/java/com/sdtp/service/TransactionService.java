package com.sdtp.service;

import com.sdtp.client.ValidatorTlsClient;
import com.sdtp.config.PlatformProperties;
import com.sdtp.model.*;
import com.sdtp.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class TransactionService {
    private final TransactionRepository repository;
    private final PlatformProperties properties;
    private final ValidatorTlsClient validatorTlsClient;
    private final ExecutorService executor;

    public TransactionService(TransactionRepository repository,
                              PlatformProperties properties,
                              ValidatorTlsClient validatorTlsClient,
                              ExecutorService executor) {
        this.repository = repository;
        this.properties = properties;
        this.validatorTlsClient = validatorTlsClient;
        this.executor = executor;
    }

    @Transactional
    public TransactionResponse process(TransactionRequest request) {
        Optional<TransactionEntity> existing = repository.findByReference(request.reference());
        if (existing.isPresent()) {
            TransactionEntity tx = existing.get();
            return new TransactionResponse(tx.getId(), tx.getReference(), tx.getStatus(), List.of());
        }

        TransactionEntity entity = new TransactionEntity();
        entity.setSender(request.sender());
        entity.setReceiver(request.receiver());
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency());
        entity.setReference(request.reference());
        entity.setStatus(TransactionStatus.PENDING);
        entity.setValidatorSummary(Map.of("decisions", List.of()));
        repository.save(entity);

        List<CompletableFuture<ValidatorDecision>> futures = properties.getValidators().stream()
                .map(v -> CompletableFuture.supplyAsync(
                        () -> validatorTlsClient.validate(v, request, properties), executor))
                .toList();

        List<ValidatorDecision> decisions = futures.stream().map(CompletableFuture::join).toList();
        long approvals = decisions.stream().filter(d -> "APPROVE".equalsIgnoreCase(d.decision())).count();
        TransactionStatus finalStatus = approvals >= ((decisions.size() / 2) + 1)
                ? TransactionStatus.APPROVED
                : TransactionStatus.REJECTED;

        entity.setStatus(finalStatus);
        entity.setValidatorSummary(Map.of("decisions", decisions));
        repository.save(entity);

        return new TransactionResponse(entity.getId(), entity.getReference(), entity.getStatus(), decisions);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String reference) {
        TransactionEntity tx = repository.findByReference(reference)
                .orElseThrow(() -> new NoSuchElementException("transaction reference not found: " + reference));
        return new TransactionResponse(tx.getId(), tx.getReference(), tx.getStatus(), List.of());
    }
}
