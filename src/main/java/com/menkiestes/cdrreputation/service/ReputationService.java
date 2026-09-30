package com.menkiestes.cdrreputation.service;

import com.menkiestes.cdrreputation.CdrReputationPlugin;
import com.menkiestes.cdrreputation.config.TierRegistry;
import com.menkiestes.cdrreputation.event.ReputationChangeEvent;
import com.menkiestes.cdrreputation.model.*;
import com.menkiestes.cdrreputation.storage.ReputationRepository;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class ReputationService {
    private final CdrReputationPlugin plugin;
    private final ReputationRepository repository;
    private final ExecutorService databaseExecutor;
    private final ConcurrentHashMap<UUID, Integer> scores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> names = new ConcurrentHashMap<>();
    private volatile TierRegistry tierRegistry;
    private volatile int defaultValue;
    private volatile int minimum;
    private volatile int maximum;

    public ReputationService(CdrReputationPlugin plugin, ReputationRepository repository, ExecutorService databaseExecutor, TierRegistry tierRegistry, int defaultValue, int minimum, int maximum) {
        this.plugin = plugin; this.repository = repository; this.databaseExecutor = databaseExecutor; this.tierRegistry = tierRegistry; this.defaultValue = defaultValue; this.minimum = minimum; this.maximum = maximum;
    }

    public void loadPlayer(UUID playerId, String playerName) {
        int loaded = repository.loadOrCreate(playerId, playerName, defaultValue);
        scores.put(playerId, clamp(loaded));
        if (playerName != null && !playerName.isBlank()) names.put(playerId, playerName);
    }

    public int getReputation(UUID playerId) { return scores.computeIfAbsent(playerId, id -> clamp(repository.loadOrCreate(id, names.get(id), defaultValue))); }
    public ReputationTier getTier(UUID playerId) { return tierRegistry.find(getReputation(playerId)); }

    public ReputationChange addReputation(UUID playerId, int amount, ReputationContext context) {
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0; use removeReputation for negative changes");
        return mutateByDelta(playerId, amount, context);
    }
    public ReputationChange removeReputation(UUID playerId, int amount, ReputationContext context) {
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        return mutateByDelta(playerId, -amount, context);
    }
    public ReputationChange setReputation(UUID playerId, int value, ReputationContext context) {
        AtomicReference<ReputationChange> result = new AtomicReference<>();
        scores.compute(playerId, (id, current) -> {
            int oldValue = current != null ? current : repository.loadOrCreate(id, names.get(id), defaultValue);
            oldValue = clamp(oldValue);
            int newValue = clamp(value);
            result.set(createChange(id, oldValue, newValue, context));
            return newValue;
        });
        persistAndPublish(result.get()); return result.get();
    }

    public CompletableFuture<List<ReputationHistoryEntry>> getHistory(UUID playerId, int limit) { return CompletableFuture.supplyAsync(() -> repository.getHistory(playerId, limit), databaseExecutor); }
    public Optional<UUID> findPlayerIdByName(String playerName) {
        return names.entrySet().stream().filter(entry -> entry.getValue().equalsIgnoreCase(playerName)).map(java.util.Map.Entry::getKey).findFirst().or(() -> repository.findPlayerIdByName(playerName));
    }
    public void updateIdentity(UUID playerId, String playerName) {
        if (playerName == null || playerName.isBlank()) return;
        names.put(playerId, playerName);
        int current = getReputation(playerId);
        CompletableFuture.runAsync(() -> repository.updateIdentity(playerId, playerName, current), databaseExecutor).exceptionally(ex -> { plugin.getLogger().log(Level.SEVERE, "Could not update reputation identity for " + playerId, ex); return null; });
    }
    public void reload(TierRegistry newRegistry, int newDefaultValue, int newMinimum, int newMaximum) {
        if (newMinimum > newMaximum) throw new IllegalArgumentException("reputation.minimum cannot be greater than reputation.maximum");
        this.tierRegistry = newRegistry; this.defaultValue = newDefaultValue; this.minimum = newMinimum; this.maximum = newMaximum;
        scores.replaceAll((uuid, value) -> clamp(value));
    }
    public void shutdown() {
        databaseExecutor.shutdown();
        try { if (!databaseExecutor.awaitTermination(5, TimeUnit.SECONDS)) databaseExecutor.shutdownNow(); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); databaseExecutor.shutdownNow(); }
    }

    private ReputationChange mutateByDelta(UUID playerId, int requestedDelta, ReputationContext context) {
        AtomicReference<ReputationChange> result = new AtomicReference<>();
        scores.compute(playerId, (id, current) -> {
            int oldValue = current != null ? current : repository.loadOrCreate(id, names.get(id), defaultValue);
            oldValue = clamp(oldValue);
            int newValue = clampLong((long) oldValue + requestedDelta);
            result.set(createChange(id, oldValue, newValue, context));
            return newValue;
        });
        persistAndPublish(result.get()); return result.get();
    }
    private ReputationChange createChange(UUID playerId, int oldValue, int newValue, ReputationContext context) {
        ReputationContext safe = context == null ? ReputationContext.of("UNSPECIFIED", "API") : context;
        return new ReputationChange(playerId, oldValue, newValue, newValue - oldValue, tierRegistry.find(oldValue), tierRegistry.find(newValue), safe, Instant.now());
    }
    private void persistAndPublish(ReputationChange change) {
        if (change == null || change.delta() == 0) return;
        CompletableFuture.runAsync(() -> repository.persistChange(change, names.get(change.playerId())), databaseExecutor).exceptionally(ex -> { plugin.getLogger().log(Level.SEVERE, "Could not persist reputation change for " + change.playerId(), ex); return null; });
        Runnable publish = () -> Bukkit.getPluginManager().callEvent(new ReputationChangeEvent(change));
        if (Bukkit.isPrimaryThread()) publish.run(); else if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, publish);
    }
    private int clamp(int value) { return Math.max(minimum, Math.min(maximum, value)); }
    private int clampLong(long value) { if (value < minimum) return minimum; if (value > maximum) return maximum; return (int) value; }
}
