package com.menkiestes.cdrreputation.config;

import com.menkiestes.cdrreputation.model.ReputationTier;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class TierRegistry {
    private final List<ReputationTier> tiers;
    private final ReputationTier fallback;

    private TierRegistry(List<ReputationTier> tiers, ReputationTier fallback) {
        this.tiers = List.copyOf(tiers);
        this.fallback = fallback;
    }

    public static TierRegistry fromConfig(FileConfiguration config) {
        List<ReputationTier> parsed = new ArrayList<>();
        for (Map<?, ?> map : config.getMapList("tiers")) {
            Object keyValue = map.get("key");
            String key = keyValue == null ? "unknown" : String.valueOf(keyValue);
            Object displayValue = map.get("display");
            String display = displayValue == null ? key : String.valueOf(displayValue);
            int min = toInt(map.get("min"), Integer.MIN_VALUE);
            int max = toInt(map.get("max"), Integer.MAX_VALUE);
            parsed.add(new ReputationTier(key, display, min, max));
        }
        parsed.sort(Comparator.comparingInt(ReputationTier::min).reversed());
        ReputationTier fallback = parsed.stream().filter(tier -> tier.contains(0)).findFirst()
                .orElse(new ReputationTier("neutral", "&fNeutral", Integer.MIN_VALUE, Integer.MAX_VALUE));
        return new TierRegistry(parsed, fallback);
    }

    public ReputationTier find(int reputation) {
        return tiers.stream().filter(tier -> tier.contains(reputation)).findFirst().orElse(fallback);
    }

    public List<ReputationTier> all() {
        return tiers;
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException ex) { return fallback; }
    }
}
