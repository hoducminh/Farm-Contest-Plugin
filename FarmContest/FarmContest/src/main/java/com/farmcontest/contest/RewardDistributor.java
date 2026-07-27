package com.farmcontest.contest;

import com.farmcontest.ConfigManager;
import com.farmcontest.DataManager;
import com.farmcontest.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RewardDistributor {

    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final DatabaseManager databaseManager;

    public RewardDistributor(ConfigManager configManager, DataManager dataManager, DatabaseManager databaseManager) {
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.databaseManager = databaseManager;
    }

    public void distributeIndividual() {
        Map<UUID, Integer> leaderboard = dataManager.getLeaderboard();
        List<Map.Entry<UUID, Integer>> sorted = leaderboard.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        int minPoints = configManager.getMinParticipationPoints();

        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Map.Entry<UUID, Integer> entry = sorted.get(i);
            if (entry.getValue() < minPoints) continue;

            int rank = i + 1;
            Player player = Bukkit.getPlayer(entry.getKey());
            String name = resolvePlayerName(entry.getKey(), player);
            databaseManager.recordContestResultAsync(entry.getKey(), name, rank);

            if (player != null) executeRewards(player, configManager.getRewards("top" + rank));
        }

        Set<UUID> top3Uuids = sorted.subList(0, Math.min(3, sorted.size()))
                .stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        for (Map.Entry<UUID, Integer> entry : leaderboard.entrySet()) {
            if (top3Uuids.contains(entry.getKey())) continue;

            Player player = Bukkit.getPlayer(entry.getKey());
            String name = resolvePlayerName(entry.getKey(), player);

            if (entry.getValue() >= minPoints) {
                databaseManager.recordContestResultAsync(entry.getKey(), name, 0);
                if (player != null) {
                    executeRewards(player, configManager.getRewards("participate"));
                    player.sendMessage(colorize(configManager.getMessage("participate-reward")
                            .replace("{points}", String.valueOf(entry.getValue()))));
                }
            } else if (player != null) {
                player.sendMessage(colorize(configManager.getMessage("not-enough")
                        .replace("{min-points}", String.valueOf(minPoints))));
            }
        }

        if (configManager.isMedalSystemEnabled()) distributeMedals(leaderboard);
    }

    private void distributeMedals(Map<UUID, Integer> leaderboard) {
        List<MedalTier> tiers = configManager.getMedalTiers();

        for (Map.Entry<UUID, Integer> entry : leaderboard.entrySet()) {
            int points = entry.getValue();
            MedalTier earned = null;
            for (MedalTier tier : tiers) {
                if (points >= tier.threshold()) { earned = tier; break; }
            }
            if (earned == null) continue;

            Player player = Bukkit.getPlayer(entry.getKey());
            String name = resolvePlayerName(entry.getKey(), player);
            databaseManager.awardMedalAsync(entry.getKey(), name, earned.id());

            if (player != null) {
                executeRewards(player, configManager.getRewards("medal-" + earned.id()));
                String msg = configManager.getMedalAwardMessage()
                        .replace("{medal}", earned.display())
                        .replace("{points}", String.valueOf(points));
                player.sendMessage(colorize(msg));
            }
        }
    }

    public void broadcastCommunityRewards(List<String> rewardCommands) {
        for (String reward : rewardCommands) {
            if (reward.contains("{player}")) {
                // Execute once per online player when {player} placeholder is used
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String command = reward.replace("{player}", p.getName())
                                          .replace("{all}", "@a");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            } else {
                String command = reward.replace("{all}", "@a");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    public void executeRewards(Player player, List<String> rewards) {
        for (String reward : rewards) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("{player}", player.getName()));
        }
    }

    private String resolvePlayerName(UUID uuid, Player onlinePlayer) {
        if (onlinePlayer != null) return onlinePlayer.getName();
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : "Unknown";
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
