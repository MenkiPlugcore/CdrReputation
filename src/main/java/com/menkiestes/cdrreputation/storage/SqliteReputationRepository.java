package com.menkiestes.cdrreputation.storage;

import com.menkiestes.cdrreputation.model.ReputationChange;
import com.menkiestes.cdrreputation.model.ReputationHistoryEntry;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteReputationRepository implements ReputationRepository {
    private final String jdbcUrl;

    public SqliteReputationRepository(JavaPlugin plugin, String fileName) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) throw new IllegalStateException("Could not create plugin data folder: " + dataFolder);
        this.jdbcUrl = "jdbc:sqlite:" + new File(dataFolder, fileName).getAbsolutePath();
    }

    @Override public void initialize() {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException ex) { throw new IllegalStateException("SQLite JDBC driver is not available", ex); }
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_reputation (uuid TEXT PRIMARY KEY, last_name TEXT, reputation INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_reputation_last_name ON player_reputation(last_name COLLATE NOCASE)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS reputation_history (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL, delta INTEGER NOT NULL, old_value INTEGER NOT NULL, new_value INTEGER NOT NULL, reason TEXT NOT NULL, source TEXT NOT NULL, actor TEXT NOT NULL, created_at INTEGER NOT NULL)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reputation_history_uuid_time ON reputation_history(uuid, created_at DESC)");
        } catch (SQLException ex) { throw new IllegalStateException("Could not initialize CdrReputation database", ex); }
    }

    @Override public int loadOrCreate(UUID playerId, String playerName, int defaultValue) {
        try (Connection connection = open()) {
            try (PreparedStatement select = connection.prepareStatement("SELECT reputation FROM player_reputation WHERE uuid = ?")) {
                select.setString(1, playerId.toString());
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        int reputation = result.getInt("reputation");
                        if (playerName != null && !playerName.isBlank()) updateIdentity(connection, playerId, playerName, reputation);
                        return reputation;
                    }
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO player_reputation(uuid, last_name, reputation, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO NOTHING")) {
                insert.setString(1, playerId.toString()); insert.setString(2, playerName); insert.setInt(3, defaultValue); insert.setLong(4, Instant.now().toEpochMilli()); insert.executeUpdate();
            }
            return defaultValue;
        } catch (SQLException ex) { throw new IllegalStateException("Could not load reputation for " + playerId, ex); }
    }

    @Override public void updateIdentity(UUID playerId, String playerName, int currentValue) {
        if (playerName == null || playerName.isBlank()) return;
        try (Connection connection = open()) { updateIdentity(connection, playerId, playerName, currentValue); }
        catch (SQLException ex) { throw new IllegalStateException("Could not update player identity for " + playerId, ex); }
    }

    private void updateIdentity(Connection connection, UUID playerId, String playerName, int currentValue) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO player_reputation(uuid, last_name, reputation, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET last_name = excluded.last_name, updated_at = excluded.updated_at")) {
            statement.setString(1, playerId.toString()); statement.setString(2, playerName); statement.setInt(3, currentValue); statement.setLong(4, Instant.now().toEpochMilli()); statement.executeUpdate();
        }
    }

    @Override public void persistChange(ReputationChange change, String playerName) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement upsert = connection.prepareStatement("INSERT INTO player_reputation(uuid, last_name, reputation, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET last_name = COALESCE(excluded.last_name, player_reputation.last_name), reputation = excluded.reputation, updated_at = excluded.updated_at")) {
                    upsert.setString(1, change.playerId().toString()); upsert.setString(2, playerName); upsert.setInt(3, change.newValue()); upsert.setLong(4, change.createdAt().toEpochMilli()); upsert.executeUpdate();
                }
                try (PreparedStatement history = connection.prepareStatement("INSERT INTO reputation_history(uuid, delta, old_value, new_value, reason, source, actor, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    history.setString(1, change.playerId().toString()); history.setInt(2, change.delta()); history.setInt(3, change.oldValue()); history.setInt(4, change.newValue()); history.setString(5, change.context().reason()); history.setString(6, change.context().source()); history.setString(7, change.context().actor()); history.setLong(8, change.createdAt().toEpochMilli()); history.executeUpdate();
                }
                connection.commit();
            } catch (SQLException ex) { connection.rollback(); throw ex; }
            finally { connection.setAutoCommit(true); }
        } catch (SQLException ex) { throw new IllegalStateException("Could not persist reputation change for " + change.playerId(), ex); }
    }

    @Override public List<ReputationHistoryEntry> getHistory(UUID playerId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<ReputationHistoryEntry> entries = new ArrayList<>();
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("SELECT id, uuid, delta, old_value, new_value, reason, source, actor, created_at FROM reputation_history WHERE uuid = ? ORDER BY created_at DESC, id DESC LIMIT ?")) {
            statement.setString(1, playerId.toString()); statement.setInt(2, safeLimit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) entries.add(new ReputationHistoryEntry(result.getLong("id"), UUID.fromString(result.getString("uuid")), result.getInt("delta"), result.getInt("old_value"), result.getInt("new_value"), result.getString("reason"), result.getString("source"), result.getString("actor"), Instant.ofEpochMilli(result.getLong("created_at"))));
            }
        } catch (SQLException ex) { throw new IllegalStateException("Could not read reputation history for " + playerId, ex); }
        return entries;
    }

    @Override public Optional<UUID> findPlayerIdByName(String playerName) {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM player_reputation WHERE last_name = ? COLLATE NOCASE LIMIT 1")) {
            statement.setString(1, playerName);
            try (ResultSet result = statement.executeQuery()) { if (result.next()) return Optional.of(UUID.fromString(result.getString("uuid"))); }
        } catch (SQLException ex) { throw new IllegalStateException("Could not resolve player name " + playerName, ex); }
        return Optional.empty();
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL"); statement.execute("PRAGMA synchronous=NORMAL"); statement.execute("PRAGMA busy_timeout=5000");
        }
        return connection;
    }

    @Override public void close() { }
}
