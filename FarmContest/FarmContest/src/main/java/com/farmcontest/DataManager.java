package com.farmcontest;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private final FarmContest plugin;
    private final File dataFile;
    private final Object saveLock = new Object();
    private final Map<UUID, Integer> contestPoints = new ConcurrentHashMap<>();

    public DataManager(FarmContest plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
        startAutoSave();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe(plugin.getConfigManager().getConsoleMessage("data-create-error").replace("{error}", e.getMessage()));
            }
        }
        FileConfiguration dataConfig;
        try {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getConfigManager().getConsoleMessage("data-create-error").replace("{error}", e.getMessage()));
            dataConfig = new YamlConfiguration();
        }
        if (dataConfig.contains("contest-points")) {
            for (String key : dataConfig.getConfigurationSection("contest-points").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int points = dataConfig.getInt("contest-points." + key);
                    contestPoints.put(uuid, Math.max(0, points));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("data-invalid-uuid-warn").replace("{key}", key));
                }
            }
        }
    }

    public void saveData() {
        synchronized (saveLock) {
            Map<UUID, Integer> snapshot = new HashMap<>(contestPoints);
            YamlConfiguration out = new YamlConfiguration();
            snapshot.forEach((uuid, pts) -> out.set("contest-points." + uuid, pts));
            try {
                File tmp = new File(dataFile.getParentFile(), "data.yml.tmp");
                out.save(tmp);
                Files.move(tmp.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                plugin.getLogger().severe(plugin.getConfigManager().getConsoleMessage("data-save-error").replace("{error}", e.getMessage()));
            }
        }
    }

    private void startAutoSave() {
        long ticks = Math.max(20L, plugin.getConfigManager().getSaveInterval().toMillis() / 50);
        new BukkitRunnable() {
            @Override
            public void run() {
                saveData();
            }
        }.runTaskTimerAsynchronously(plugin, ticks, ticks);
    }

    public void addPoints(UUID playerUuid, int points) {
        if (playerUuid == null) return;
        contestPoints.merge(playerUuid, points, Integer::sum);
    }

    public void removePoints(UUID playerUuid, int points) {
        if (playerUuid == null) return;
        contestPoints.compute(playerUuid, (k, v) -> Math.max(0, (v == null ? 0 : v) - points));
    }

    public void setPoints(UUID playerUuid, int points) {
        if (playerUuid == null) return;
        contestPoints.put(playerUuid, Math.max(0, points));
    }

    public int getPoints(UUID playerUuid) {
        if (playerUuid == null) return 0;
        return contestPoints.getOrDefault(playerUuid, 0);
    }

    public Map<UUID, Integer> getLeaderboard() {
        return new HashMap<>(contestPoints);
    }

    public void clearContestData() {
        contestPoints.clear();
        saveData();
    }
}
