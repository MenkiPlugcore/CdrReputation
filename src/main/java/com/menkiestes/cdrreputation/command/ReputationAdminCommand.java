package com.menkiestes.cdrreputation.command;

import com.menkiestes.cdrreputation.CdrReputationPlugin;
import com.menkiestes.cdrreputation.model.*;
import com.menkiestes.cdrreputation.service.ReputationService;
import com.menkiestes.cdrreputation.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ReputationAdminCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("get", "set", "add", "remove", "history", "reload");
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.of("Asia/Jakarta"));
    private final CdrReputationPlugin plugin;
    private final ReputationService service;
    private final MessageService messages;

    public ReputationAdminCommand(CdrReputationPlugin plugin, ReputationService service, MessageService messages) { this.plugin = plugin; this.service = service; this.messages = messages; }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cdrreputation.admin")) { messages.send(sender, "no-permission"); return true; }
        if (args.length == 0) { messages.send(sender, "usage"); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) { plugin.reloadPluginConfiguration(); messages.send(sender, "reloaded"); return true; }
        if (args.length < 2) { messages.send(sender, "usage"); return true; }
        Target target = resolveTarget(args[1]);
        if (target == null) { messages.send(sender, "player-not-found"); return true; }
        return switch (sub) {
            case "get" -> handleGet(sender, target);
            case "set", "add", "remove" -> handleMutation(sender, sub, target, args);
            case "history" -> handleHistory(sender, target, args);
            default -> { messages.send(sender, "usage"); yield true; }
        };
    }

    private boolean handleGet(CommandSender sender, Target target) {
        int reputation = service.getReputation(target.uuid());
        messages.send(sender, "get", Map.of("player", target.name(), "reputation", String.valueOf(reputation), "tier", service.getTier(target.uuid()).displayName()));
        return true;
    }
    private boolean handleMutation(CommandSender sender, String sub, Target target, String[] args) {
        if (args.length < 3) { messages.send(sender, "usage"); return true; }
        int amount;
        try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { messages.send(sender, "invalid-number"); return true; }
        if ((sub.equals("add") || sub.equals("remove")) && amount < 0) { messages.send(sender, "invalid-number"); return true; }
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Admin adjustment";
        ReputationContext context = ReputationContext.admin(reason, sender.getName());
        ReputationChange change = switch (sub) {
            case "set" -> service.setReputation(target.uuid(), amount, context);
            case "add" -> service.addReputation(target.uuid(), amount, context);
            case "remove" -> service.removeReputation(target.uuid(), amount, context);
            default -> throw new IllegalStateException("Unexpected subcommand: " + sub);
        };
        messages.send(sender, "changed", Map.of("player", target.name(), "old", String.valueOf(change.oldValue()), "new", String.valueOf(change.newValue()), "delta", signed(change.delta())));
        return true;
    }
    private boolean handleHistory(CommandSender sender, Target target, String[] args) {
        int limit = 10;
        if (args.length >= 3) {
            try { limit = Math.max(1, Math.min(50, Integer.parseInt(args[2]))); }
            catch (NumberFormatException ex) { messages.send(sender, "invalid-number"); return true; }
        }
        service.getHistory(target.uuid(), limit).whenComplete((history, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (throwable != null) { sender.sendMessage(messages.raw("&cGagal membaca riwayat reputasi. Cek console server.")); plugin.getLogger().severe("Could not read reputation history: " + throwable.getMessage()); return; }
            sendHistory(sender, target, history);
        }));
        return true;
    }
    private void sendHistory(CommandSender sender, Target target, List<ReputationHistoryEntry> history) {
        if (history.isEmpty()) { messages.send(sender, "history-empty"); return; }
        messages.send(sender, "history-header", Map.of("player", target.name(), "count", String.valueOf(history.size())));
        for (ReputationHistoryEntry entry : history) messages.send(sender, "history-line", Map.of("time", HISTORY_TIME.format(entry.createdAt()), "delta", signed(entry.delta()), "new", String.valueOf(entry.newValue()), "source", entry.source(), "reason", entry.reason()));
    }
    private Target resolveTarget(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return new Target(online.getUniqueId(), online.getName());
        try { return new Target(UUID.fromString(input), input); } catch (IllegalArgumentException ignored) { }
        Optional<UUID> stored = service.findPlayerIdByName(input);
        return stored.map(uuid -> new Target(uuid, input)).orElse(null);
    }
    private static String signed(int value) { return value > 0 ? "+" + value : String.valueOf(value); }

    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("cdrreputation.admin")) return List.of();
        if (args.length == 1) return filter(SUBCOMMANDS, args[0]);
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        return List.of();
    }
    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT); List<String> result = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        return result;
    }
    private record Target(UUID uuid, String name) { }
}
