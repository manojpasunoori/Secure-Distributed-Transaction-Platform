package com.sdtp.model;

import java.util.List;
import java.util.UUID;

public record TransactionResponse(UUID id, String reference, TransactionStatus status, List<ValidatorDecision> decisions) {
}
