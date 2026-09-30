package com.menkiestes.cdrreputation.listener;

import com.menkiestes.cdrreputation.CdrReputationPlugin;
import com.menkiestes.cdrreputation.service.ReputationService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.logging.Level;

public final class PlayerLifecycleListener implements Listener {
    private final CdrReputationPlugin plugin;
    private final ReputationService service;
    public PlayerLifecycleListener(CdrReputationPlugin plugin, ReputationService service) { this.plugin = plugin; this.service = service; }
    @EventHandler public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        try { service.loadPlayer(event.getUniqueId(), event.getName()); }
        catch (RuntimeException ex) { plugin.getLogger().log(Level.SEVERE, "Could not preload reputation for " + event.getName() + " (" + event.getUniqueId() + ")", ex); }
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { service.updateIdentity(event.getPlayer().getUniqueId(), event.getPlayer().getName()); }
}
