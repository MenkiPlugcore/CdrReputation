package com.menkiestes.cdrreputation;

import com.menkiestes.cdrreputation.api.*;
import com.menkiestes.cdrreputation.command.ReputationAdminCommand;
import com.menkiestes.cdrreputation.config.TierRegistry;
import com.menkiestes.cdrreputation.listener.PlayerLifecycleListener;
import com.menkiestes.cdrreputation.service.ReputationService;
import com.menkiestes.cdrreputation.storage.*;
import com.menkiestes.cdrreputation.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.*;

public final class CdrReputationPlugin extends JavaPlugin {
    private ReputationRepository repository;
    private ReputationService reputationService;
    private MessageService messages;

    @Override public void onEnable() {
        saveDefaultConfig();
        String databaseFile = getConfig().getString("storage.file", "reputation.db");
        repository = new SqliteReputationRepository(this, databaseFile);
        repository.initialize();
        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor(new DatabaseThreadFactory());
        TierRegistry tiers = TierRegistry.fromConfig(getConfig());
        int defaultValue = getConfig().getInt("reputation.default", 0);
        int minimum = getConfig().getInt("reputation.minimum", -5000);
        int maximum = getConfig().getInt("reputation.maximum", 5000);
        validateBounds(minimum, maximum);
        reputationService = new ReputationService(this, repository, databaseExecutor, tiers, defaultValue, minimum, maximum);
        messages = new MessageService(getConfig());
        CdrReputationApi api = new DefaultCdrReputationApi(reputationService);
        Bukkit.getServicesManager().register(CdrReputationApi.class, api, this, ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(this, reputationService), this);
        ReputationAdminCommand adminCommand = new ReputationAdminCommand(this, reputationService, messages);
        PluginCommand command = getCommand("cdrrep");
        if (command == null) throw new IllegalStateException("Command cdrrep is missing from plugin.yml");
        command.setExecutor(adminCommand); command.setTabCompleter(adminCommand);
        Bukkit.getOnlinePlayers().forEach(player -> reputationService.loadPlayer(player.getUniqueId(), player.getName()));
        getLogger().info("CdrReputation v" + getPluginMeta().getVersion() + " enabled. SQLite storage ready.");
    }

    @Override public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        if (reputationService != null) reputationService.shutdown();
        if (repository != null) repository.close();
    }

    public void reloadPluginConfiguration() {
        reloadConfig();
        int minimum = getConfig().getInt("reputation.minimum", -5000);
        int maximum = getConfig().getInt("reputation.maximum", 5000);
        validateBounds(minimum, maximum);
        reputationService.reload(TierRegistry.fromConfig(getConfig()), getConfig().getInt("reputation.default", 0), minimum, maximum);
        messages.reload(getConfig());
    }

    private static void validateBounds(int minimum, int maximum) {
        if (minimum > maximum) throw new IllegalArgumentException("reputation.minimum cannot be greater than reputation.maximum");
    }
    private static final class DatabaseThreadFactory implements ThreadFactory {
        @Override public Thread newThread(Runnable runnable) { Thread thread = new Thread(runnable, "CdrReputation-Database"); thread.setDaemon(true); return thread; }
    }
}
