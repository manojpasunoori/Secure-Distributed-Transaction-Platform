package com.sdtp.controller;

import com.sdtp.model.TransactionRequest;
import com.sdtp.model.TransactionResponse;
import com.sdtp.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "coordinator");
    }

    @GetMapping("/transactions/{reference}")
    public TransactionResponse getByReference(@PathVariable String reference) {
        return transactionService.getByReference(reference);
    }

    @PostMapping("/transactions")
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return transactionService.process(request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NoSuchElementException e) {
        return Map.of("error", e.getMessage());
    }
}
