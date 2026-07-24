package com.farmcontest;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private final FarmContest plugin;
    private Connection connection;

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
        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.getConfigManager().getConsoleMessage("db-table-error").replace("{error}", e.getMessage()));
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

    /**
     * Tăng số lượt thu hoạch của player — gọi async từ FarmListener.
     */
    public void incrementHarvestAsync(UUID uuid, String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!isAvailable()) return;
            try {
                ensureProfile(uuid, playerName);
                String sql = "UPDATE farm_profiles SET total_harvests = total_harvests + 1 WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-harvest-error").replace("{error}", e.getMessage()));
            }
        });
    }

    /**
     * Ghi kết quả cuộc thi cho player — gọi từ main thread sau khi cuộc thi kết thúc.
     *
     * @param rank 1 = Top 1, 2 = Top 2, 3 = Top 3, 0 = chỉ tham gia
     */
    public void recordContestResult(UUID uuid, String playerName, int rank) {
        if (!isAvailable()) return;
        try {
            ensureProfile(uuid, playerName);
            StringBuilder sql = new StringBuilder(
                "UPDATE farm_profiles SET total_contests = total_contests + 1");
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
    }

    /**
     * Lấy hồ sơ nông dân. Trả về profile rỗng nếu chưa có dữ liệu.
     */
    public FarmProfile getProfile(UUID uuid) {
        if (!isAvailable()) return new FarmProfile(0, 0, 0, 0, 0);
        String sql = "SELECT * FROM farm_profiles WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FarmProfile(
                        rs.getInt("total_contests"),
                        rs.getInt("top1_wins"),
                        rs.getInt("top2_wins"),
                        rs.getInt("top3_wins"),
                        rs.getLong("total_harvests")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("db-profile-error").replace("{error}", e.getMessage()));
        }
        return new FarmProfile(0, 0, 0, 0, 0);
    }

    /** Đóng kết nối khi plugin tắt. */
    public void close() {
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
