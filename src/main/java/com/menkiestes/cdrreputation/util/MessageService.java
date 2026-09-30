package com.menkiestes.cdrreputation.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public final class MessageService {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private FileConfiguration config;
    public MessageService(FileConfiguration config) { this.config = config; }
    public void reload(FileConfiguration config) { this.config = config; }
    public void send(CommandSender sender, String key) { send(sender, key, Map.of()); }
    public void send(CommandSender sender, String key, Map<String, String> replacements) { sender.sendMessage(component(key, replacements)); }
    public Component component(String key, Map<String, String> replacements) {
        String prefix = config.getString("messages.prefix", "");
        String raw = config.getString("messages." + key, "&cMissing message: " + key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        return LEGACY.deserialize(prefix + raw);
    }
    public Component raw(String value) { return LEGACY.deserialize(value == null ? "" : value); }
}
