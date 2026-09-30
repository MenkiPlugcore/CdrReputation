package com.menkiestes.cdrreputation.storage;

import com.menkiestes.cdrreputation.model.ReputationChange;
import com.menkiestes.cdrreputation.model.ReputationHistoryEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReputationRepository extends AutoCloseable {
    void initialize();
    int loadOrCreate(UUID playerId, String playerName, int defaultValue);
    void updateIdentity(UUID playerId, String playerName, int currentValue);
    void persistChange(ReputationChange change, String playerName);
    List<ReputationHistoryEntry> getHistory(UUID playerId, int limit);
    Optional<UUID> findPlayerIdByName(String playerName);
    @Override void close();
}
