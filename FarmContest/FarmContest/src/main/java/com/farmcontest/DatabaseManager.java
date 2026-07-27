package com.farmcontest;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DatabaseManager {

    private final FarmContest plugin;
    private Connection connection;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "FarmContest-DB"));

    private static final String TABLE_PROFILES =
        "CREATE TABLE IF NOT EXISTS farm_profiles (" +
        "  uuid          TEXT PRIMARY KEY," +
        "  player_name   TEXT NOT NULL," +
        "  total_contests INTEGER DEFAULT 0," +
        "  top1_wins     INTEGER DEFAULT 0," +
        "  top2_wins     INTEGER DEFAULT 0," +
        "  top3_wins     INTEGER DEFAULT 0," +
        "  total_harvests INTEGER DEFAULT 0" +
        ")";

    private static final String TABLE_MEDALS =
        "CREATE TABLE IF NOT EXISTS farm_medals (" +
        "  uuid TEXT NOT NULL," +
        "  tier TEXT NOT NULL," +
        "  count INTEGER DEFAULT 0," +
        "  PRIMARY KEY (uuid, tier)" +
        ")";

    public DatabaseManager(FarmContest plugin) {
        this.plugin = plugin;
        initConnection();
        initTable();
    }

    private void initConnection() {
        plugin.getDataFolder().mkdirs();
        File dbFile = new File(plugin.getDataFolder(), "farmcontest.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        try {
            connection = DriverManager.getConnection(url);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }
            plugin.getLogger().info(plugin.getConfigManager().getConsoleMessage("db-connected").replace("{file}", dbFile.getName()));
        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.getConfigManager().getConsoleMessage("db-connect-error").replace("{error}", e.getMessage()));
        }
    }

    private void initTable() {
        if (connection == null) return;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(TABLE_PROFILES);
            stmt.execute(TABLE_MEDALS);
        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.getConfigManager().getConsoleMessage("db-table-error").replace("{error}", e.getMessage()));
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE farm_profiles ADD COLUMN total_mob_kills INTEGER DEFAULT 0");
        } catch (SQLException ignored) {
        }
    }

    private boolean isAvailable() {
        return connection != null;
    }

    private void ensureProfile(UUID uuid, String name) throws SQLException {
        String sql = "INSERT OR IGNORE INTO farm_profiles (uuid, player_name) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        }
        String updateName = "UPDATE farm_profiles SET player_name = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateName)) {
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void incrementHarvestAsync(UUID uuid, String playerName, int count) {
        if (uuid == null || count <= 0) return;
        dbExecutor.submit(() -> {
            if (!isAvailable()) return;
            try {
                ensureProfile(uuid, playerName);
                String sql = "UPDATE farm_profiles SET total_harvests = total_harvests + ? WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, count);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-harvest-error").replace("{error}", e.getMessage()));
            }
        });
    }

    public void incrementMobKillAsync(UUID uuid, String playerName, int count) {
        if (uuid == null || count <= 0) return;
        dbExecutor.submit(() -> {
            if (!isAvailable()) return;
            try {
                ensureProfile(uuid, playerName);
                String sql = "UPDATE farm_profiles SET total_mob_kills = total_mob_kills + ? WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, count);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-harvest-error").replace("{error}", e.getMessage()));
            }
        });
    }

    public void recordContestResultAsync(UUID uuid, String playerName, int rank) {
        if (uuid == null) return;
        dbExecutor.submit(() -> {
            if (!isAvailable()) return;
            try {
                ensureProfile(uuid, playerName);
                StringBuilder sql = new StringBuilder("UPDATE farm_profiles SET total_contests = total_contests + 1");
                if (rank == 1) sql.append(", top1_wins = top1_wins + 1");
                if (rank == 2) sql.append(", top2_wins = top2_wins + 1");
                if (rank == 3) sql.append(", top3_wins = top3_wins + 1");
                sql.append(" WHERE uuid = ?");
                try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-result-error").replace("{error}", e.getMessage()));
            }
        });
    }

    public void awardMedalAsync(UUID uuid, String playerName, String medalTier) {
        if (uuid == null || medalTier == null) return;
        dbExecutor.submit(() -> {
            if (!isAvailable()) return;
            try {
                ensureProfile(uuid, playerName);
                String sql = "INSERT INTO farm_medals (uuid, tier, count) VALUES (?, ?, 1) " +
                        "ON CONFLICT(uuid, tier) DO UPDATE SET count = count + 1";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, medalTier);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-medal-error").replace("{error}", e.getMessage()));
            }
        });
    }

    public void getProfileAsync(UUID uuid, Consumer<FarmProfile> callback) {
        if (uuid == null || !isAvailable()) {
            callback.accept(emptyProfile());
            return;
        }
        dbExecutor.submit(() -> {
            FarmProfile profile = queryProfile(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(profile));
        });
    }

    private FarmProfile queryProfile(UUID uuid) {
        String sql = "SELECT * FROM farm_profiles WHERE uuid = ?";
        java.util.Map<String, Integer> medals = queryMedals(uuid);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FarmProfile(
                        rs.getInt("total_contests"),
                        rs.getInt("top1_wins"),
                        rs.getInt("top2_wins"),
                        rs.getInt("top3_wins"),
                        rs.getLong("total_harvests"),
                        rs.getLong("total_mob_kills"),
                        medals
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-profile-error").replace("{error}", e.getMessage()));
        }
        return emptyProfile();
    }

    private java.util.Map<String, Integer> queryMedals(UUID uuid) {
        java.util.Map<String, Integer> medals = new java.util.LinkedHashMap<>();
        String sql = "SELECT tier, count FROM farm_medals WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) medals.put(rs.getString("tier"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-profile-error").replace("{error}", e.getMessage()));
        }
        return medals;
    }

    private FarmProfile emptyProfile() {
        return new FarmProfile(0, 0, 0, 0, 0, 0, java.util.Map.of());
    }

    public void close() {
        dbExecutor.shutdown();
        try {
            dbExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info(plugin.getConfigManager().getConsoleMessage("db-closed"));
            } catch (SQLException e) {
                plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-close-error").replace("{error}", e.getMessage()));
            }
        }
    }
}
