package com.farmcontest;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * For Developer.
 *
 * Placeholders:
 *   %farmcontest_time_left%          — Time left (MM:SS) or fallback
 *   %farmcontest_top_1_name%         — Name player Top 1
 *   %farmcontest_top_1_points%       — Point Top 1
 *   ... (top_2 đến top_10 tương tự)
 *   %farmcontest_player_points%      — Self Point
 *   %farmcontest_active%             — "true" / "false"
 */
public class FarmContestExpansion extends PlaceholderExpansion {

    private final FarmContest plugin;
    private final ContestManager contestManager;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public FarmContestExpansion(FarmContest plugin) {
        this.plugin = plugin;
        this.contestManager = plugin.getContestManager();
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public @NotNull String getIdentifier() { return "farmcontest"; }

    @Override
    public @NotNull String getAuthor() { return "_MintBae_"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public boolean canRegister() { return true; }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        // ── %farmcontest_active% ──────────────────────────────────
        if (params.equalsIgnoreCase("active")) {
            return contestManager.isContestActive() ? "true" : "false";
        }

        // ── %farmcontest_time_left% ───────────────────────────────
        if (params.equalsIgnoreCase("time_left")) {
            if (!contestManager.isContestActive()) {
                return configManager.getPapiTimeLeftFallback();
            }
            LocalDateTime endTime = contestManager.getContestEndTime();
            if (endTime == null) return configManager.getPapiTimeLeftFallback();
            Duration remaining = Duration.between(LocalDateTime.now(), endTime);
            if (remaining.isNegative()) return configManager.getPapiTimeLeftFallback();
            long minutes = remaining.toMinutes();
            long seconds = remaining.toSecondsPart();
            return String.format("%02d:%02d", minutes, seconds);
        }

        // ── %farmcontest_player_points% ───────────────────────────
        if (params.equalsIgnoreCase("player_points")) {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) {
                return configManager.getPapiPlayerPointsFallback();
            }
            if (!contestManager.isContestActive()) {
                return configManager.getPapiPlayerPointsFallback();
            }
            Player player = offlinePlayer.getPlayer();
            if (player == null) return configManager.getPapiPlayerPointsFallback();
            return String.valueOf(dataManager.getPoints(player.getUniqueId()));
        }

        // ── %farmcontest_top_N_name% / %farmcontest_top_N_points% ─
        if (params.startsWith("top_")) {
            String[] parts = params.split("_"); // ["top", "N", "name"/"points"]
            if (parts.length == 3) {
                int rank;
                try {
                    rank = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (rank < 1 || rank > 10) return null;

                List<Map.Entry<UUID, Integer>> sorted = getSortedLeaderboard();
                if (rank > sorted.size()) {
                    String field = parts[2];
                    if (field.equalsIgnoreCase("name"))   return configManager.getPapiTopNameFallback();
                    if (field.equalsIgnoreCase("points")) return configManager.getPapiTopPointsFallback();
                    return null;
                }

                Map.Entry<UUID, Integer> entry = sorted.get(rank - 1);
                String field = parts[2];

                if (field.equalsIgnoreCase("name")) {
                    org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(entry.getKey());
                    String name = op.getName();
                    return name != null ? name : "Unknown";
                }
                if (field.equalsIgnoreCase("points")) {
                    return String.valueOf(entry.getValue());
                }
            }
        }

        return null;
    }

    // ─────────────────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────────────────

    private List<Map.Entry<UUID, Integer>> getSortedLeaderboard() {
        if (!contestManager.isContestActive()) return Collections.emptyList();
        return dataManager.getLeaderboard().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
}
