package com.menkiestes.cdrreputation.api;

import com.menkiestes.cdrreputation.model.ReputationChange;
import com.menkiestes.cdrreputation.model.ReputationContext;
import com.menkiestes.cdrreputation.model.ReputationHistoryEntry;
import com.menkiestes.cdrreputation.model.ReputationTier;
import com.menkiestes.cdrreputation.service.ReputationService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DefaultCdrReputationApi implements CdrReputationApi {
    private final ReputationService service;
    public DefaultCdrReputationApi(ReputationService service) { this.service = service; }
    @Override public int getReputation(UUID playerId) { return service.getReputation(playerId); }
    @Override public ReputationTier getTier(UUID playerId) { return service.getTier(playerId); }
    @Override public ReputationChange addReputation(UUID playerId, int amount, ReputationContext context) { return service.addReputation(playerId, amount, context); }
    @Override public ReputationChange removeReputation(UUID playerId, int amount, ReputationContext context) { return service.removeReputation(playerId, amount, context); }
    @Override public ReputationChange setReputation(UUID playerId, int value, ReputationContext context) { return service.setReputation(playerId, value, context); }
    @Override public CompletableFuture<List<ReputationHistoryEntry>> getHistory(UUID playerId, int limit) { return service.getHistory(playerId, limit); }
}
