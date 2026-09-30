package com.menkiestes.cdrreputation.model;

import java.time.Instant;
import java.util.UUID;

public record ReputationHistoryEntry(long id, UUID playerId, int delta, int oldValue, int newValue,
                                     String reason, String source, String actor, Instant createdAt) {
}
