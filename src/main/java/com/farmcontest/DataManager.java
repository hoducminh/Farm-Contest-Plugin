
package com.farmcontest;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final FarmContest plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private Map<UUID, Integer> contestPoints = new HashMap<>();

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

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load contest points
        if (dataConfig.contains("contest-points")) {
            for (String key : dataConfig.getConfigurationSection("contest-points").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int points = dataConfig.getInt("contest-points." + key);
                    contestPoints.put(uuid, points);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(plugin.getConfigManager().getConsoleMessage("data-invalid-uuid-warn").replace("{key}", key));
                }
            }
        }
    }

    public void saveData() {
        // Save contest points
        dataConfig.set("contest-points", null); // Clear existing data
        for (Map.Entry<UUID, Integer> entry : contestPoints.entrySet()) {
            dataConfig.set("contest-points." + entry.getKey().toString(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe(plugin.getConfigManager().getConsoleMessage("data-save-error").replace("{error}", e.getMessage()));
        }
    }

    private void startAutoSave() {
        long saveInterval = plugin.getConfigManager().getSaveInterval().toMillis() / 50; // Convert to ticks

        new BukkitRunnable() {
            @Override
            public void run() {
                saveData();
            }
        }.runTaskTimerAsynchronously(plugin, saveInterval, saveInterval);
    }

    public void addPoints(UUID playerUuid, int points) {
        contestPoints.put(playerUuid, contestPoints.getOrDefault(playerUuid, 0) + points);
    }

    public void removePoints(UUID playerUuid, int points) {
        int currentPoints = contestPoints.getOrDefault(playerUuid, 0);
        contestPoints.put(playerUuid, Math.max(0, currentPoints - points));
    }

    public void setPoints(UUID playerUuid, int points) {
        contestPoints.put(playerUuid, Math.max(0, points));
    }

    public int getPoints(UUID playerUuid) {
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
