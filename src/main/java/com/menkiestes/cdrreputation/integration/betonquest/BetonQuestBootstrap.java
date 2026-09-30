package com.menkiestes.cdrreputation.integration.betonquest;

import com.menkiestes.cdrreputation.CdrReputationPlugin;
import org.betonquest.betonquest.api.integration.IntegrationService;

public final class BetonQuestBootstrap {
    private BetonQuestBootstrap() {
    }

    public static boolean register(CdrReputationPlugin plugin) {
        IntegrationService integrationService = plugin.getServer().getServicesManager().load(IntegrationService.class);
        if (integrationService == null) {
            plugin.getLogger().warning("BetonQuest terdeteksi tetapi IntegrationService belum tersedia. Hook BetonQuest dilewati.");
            return false;
        }

        integrationService.withPolicies().register(plugin, () -> new CdrReputationBetonQuestIntegration());
        return true;
    }
}
