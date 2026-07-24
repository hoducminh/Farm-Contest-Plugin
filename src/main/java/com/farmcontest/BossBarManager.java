package com.farmcontest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BossBarManager {
    private final FarmContest plugin;
    private final ConfigManager configManager;
    private final Map<Player, BossBar> playerBossBars = new HashMap<>();

    public BossBarManager(FarmContest plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void createContestBossBar(Set<Material> selectedCrops) {
        if (!configManager.isBossBarEnabled()) return;

        String cropsText = selectedCrops.stream()
                .map(configManager::getCropDisplayName)
                .collect(Collectors.joining(", "));

        String bossBarText = configManager.getBossBarText()
                .replace("{crops}", cropsText)
                .replace("{time}", "00:00");

        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bossBar = Bukkit.createBossBar(
                    ChatColor.translateAlternateColorCodes('&', bossBarText),
                    configManager.getBossBarColor(),
                    configManager.getBossBarStyle()
            );

            bossBar.addPlayer(player);
            bossBar.setProgress(1.0);
            playerBossBars.put(player, bossBar);
        }
    }

    public void updateBossBar(Set<Material> selectedCrops, LocalDateTime contestEndTime) {
        if (!configManager.isBossBarEnabled()) return;

        LocalDateTime now = LocalDateTime.now();
        Duration remaining = Duration.between(now, contestEndTime);
        Duration total = configManager.getContestDuration();

        double progress = Math.max(0.0, (double) remaining.toMillis() / total.toMillis());
        String timeLeft = formatTimeLeft(remaining);

        String cropsText = selectedCrops.stream()
                .map(configManager::getCropDisplayName)
                .collect(Collectors.joining(", "));

        String bossBarText = configManager.getBossBarText()
                .replace("{crops}", cropsText)
                .replace("{time}", timeLeft);

        for (Map.Entry<Player, BossBar> entry : playerBossBars.entrySet()) {
            Player player = entry.getKey();
            BossBar bossBar = entry.getValue();

            if (player.isOnline()) {
                bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', bossBarText));
                bossBar.setProgress(progress);
            } else {
                bossBar.removeAll();
                playerBossBars.remove(player);
            }
        }
    }

    public void addPlayerBossBar(Player player, Set<Material> selectedCrops, LocalDateTime contestEndTime) {
        if (!configManager.isBossBarEnabled() || playerBossBars.containsKey(player)) return;

        LocalDateTime now = LocalDateTime.now();
        Duration remaining = Duration.between(now, contestEndTime);
        Duration total = configManager.getContestDuration();

        double progress = Math.max(0.0, (double) remaining.toMillis() / total.toMillis());
        String timeLeft = formatTimeLeft(remaining);

        String cropsText = selectedCrops.stream()
                .map(configManager::getCropDisplayName)
                .collect(Collectors.joining(", "));

        String bossBarText = configManager.getBossBarText()
                .replace("{crops}", cropsText)
                .replace("{time}", timeLeft);

        BossBar bossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', bossBarText),
                configManager.getBossBarColor(),
                configManager.getBossBarStyle()
        );

        bossBar.addPlayer(player);
        bossBar.setProgress(progress);
        playerBossBars.put(player, bossBar);
    }

    public void removePlayerBossBar(Player player) {
        BossBar bossBar = playerBossBars.remove(player);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void removeAllBossBars() {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        playerBossBars.clear();
    }

    private String formatTimeLeft(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d", minutes, seconds);
    }
}