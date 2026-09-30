package com.menkiestes.cdrreputation.api;

import org.bukkit.Bukkit;

public final class CdrReputationProvider {
    private CdrReputationProvider() {}
    public static CdrReputationApi get() {
        var registration = Bukkit.getServicesManager().getRegistration(CdrReputationApi.class);
        if (registration == null) throw new IllegalStateException("CdrReputation API is not registered");
        return registration.getProvider();
    }
}
