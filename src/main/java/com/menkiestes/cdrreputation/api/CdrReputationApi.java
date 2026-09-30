package com.menkiestes.cdrreputation.api;

import com.menkiestes.cdrreputation.model.ReputationChange;
import com.menkiestes.cdrreputation.model.ReputationContext;
import com.menkiestes.cdrreputation.model.ReputationHistoryEntry;
import com.menkiestes.cdrreputation.model.ReputationTier;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CdrReputationApi {
    int getReputation(UUID playerId);
    ReputationTier getTier(UUID playerId);
    ReputationChange addReputation(UUID playerId, int amount, ReputationContext context);
    ReputationChange removeReputation(UUID playerId, int amount, ReputationContext context);
    ReputationChange setReputation(UUID playerId, int value, ReputationContext context);
    CompletableFuture<List<ReputationHistoryEntry>> getHistory(UUID playerId, int limit);
}
