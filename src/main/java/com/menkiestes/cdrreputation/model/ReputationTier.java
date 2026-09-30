package com.menkiestes.cdrreputation.model;

public record ReputationTier(String key, String displayName, int min, int max) {
    public boolean contains(int value) {
        return value >= min && value <= max;
    }
}
