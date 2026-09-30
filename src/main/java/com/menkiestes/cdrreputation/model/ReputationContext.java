package com.menkiestes.cdrreputation.model;

public record ReputationContext(String reason, String source, String actor) {
    public ReputationContext {
        reason = normalize(reason, "UNSPECIFIED");
        source = normalize(source, "API");
        actor = normalize(actor, "SYSTEM");
    }

    public static ReputationContext of(String reason, String source) {
        return new ReputationContext(reason, source, "SYSTEM");
    }

    public static ReputationContext admin(String reason, String actor) {
        return new ReputationContext(reason, "ADMIN", actor);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }
}
