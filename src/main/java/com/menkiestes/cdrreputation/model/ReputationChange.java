package com.menkiestes.cdrreputation.model;

import java.time.Instant;
import java.util.UUID;

public record ReputationChange(UUID playerId, int oldValue, int newValue, int delta,
                               ReputationTier oldTier, ReputationTier newTier,
                               ReputationContext context, Instant createdAt) {
    public boolean tierChanged() {
        return !oldTier.key().equalsIgnoreCase(newTier.key());
    }
}
