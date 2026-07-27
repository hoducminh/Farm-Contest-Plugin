package com.farmcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class BossBarManager {

    private final ConfigManager configManager;
    private final Map<Player, BossBar> playerBossBars = new HashMap<>();

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    public BossBarManager(FarmContest plugin) {
        this.configManager = plugin.getConfigManager();
    }

    public void createContestBossBar(String targetsText) {
        if (!configManager.isBossBarEnabled()) return;

        String text = buildBossBarText(targetsText, "00:00");

        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bossBar = Bukkit.createBossBar(
                    toLegacyString(text),
                    configManager.getBossBarColor(),
                    configManager.getBossBarStyle()
            );
            bossBar.addPlayer(player);
            bossBar.setProgress(1.0);
            playerBossBars.put(player, bossBar);
        }
    }

    public void updateBossBar(String targetsText, LocalDateTime contestEndTime) {
        if (!configManager.isBossBarEnabled()) return;

        LocalDateTime now  = LocalDateTime.now();
        Duration remaining = Duration.between(now, contestEndTime);
        Duration total     = configManager.getContestDuration();
        double progress    = clampProgress(remaining, total);
        String timeLeft    = formatTimeLeft(remaining);
        String text        = buildBossBarText(targetsText, timeLeft);
        String legacyTitle = toLegacyString(text);

        for (Map.Entry<Player, BossBar> entry : Map.copyOf(playerBossBars).entrySet()) {
            Player player   = entry.getKey();
            BossBar bossBar = entry.getValue();

            if (player.isOnline()) {
                bossBar.setTitle(legacyTitle);
                bossBar.setProgress(progress);
            } else {
                bossBar.removeAll();
                playerBossBars.remove(player);
            }
        }
    }

    public void addPlayerBossBar(Player player, String targetsText, LocalDateTime contestEndTime) {
        if (!configManager.isBossBarEnabled() || playerBossBars.containsKey(player)) return;

        LocalDateTime now  = LocalDateTime.now();
        Duration remaining = Duration.between(now, contestEndTime);
        Duration total     = configManager.getContestDuration();
        double progress    = clampProgress(remaining, total);
        String timeLeft    = formatTimeLeft(remaining);
        String text        = buildBossBarText(targetsText, timeLeft);

        BossBar bossBar = Bukkit.createBossBar(
                toLegacyString(text),
                configManager.getBossBarColor(),
                configManager.getBossBarStyle()
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(progress);
        playerBossBars.put(player, bossBar);
    }

    public void removePlayerBossBar(Player player) {
        BossBar bossBar = playerBossBars.remove(player);
        if (bossBar != null) bossBar.removeAll();
    }

    public void removeAllBossBars() {
        playerBossBars.values().forEach(BossBar::removeAll);
        playerBossBars.clear();
    }

    private String buildBossBarText(String targetsText, String timeLeft) {
        return configManager.getBossBarText()
                .replace("{crops}", targetsText)
                .replace("{time}", timeLeft);
    }

    private static String toLegacyString(String ampersandText) {
        Component component = LEGACY.deserialize(ampersandText);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    private static double clampProgress(Duration remaining, Duration total) {
        if (total == null || total.isZero() || total.isNegative()) return 0.0;
        double p = (double) remaining.toMillis() / total.toMillis();
        return Math.max(0.0, Math.min(1.0, p));
    }

    private static String formatTimeLeft(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d", minutes, seconds);
    }
}
