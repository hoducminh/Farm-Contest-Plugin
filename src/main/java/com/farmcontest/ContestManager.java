package com.farmcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ContestManager {
    private final FarmContest plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final BossBarManager bossBarManager;

    private boolean contestActive = false;
    private LocalDateTime contestStartTime;
    private LocalDateTime contestEndTime;
    private Set<Material> selectedCrops = new HashSet<>();
    private Material luckyCrop = null;   // Cây may mắn của phiên này
    private BukkitTask schedulerTask;
    private BukkitTask countdownTask;

    public ContestManager(FarmContest plugin, ConfigManager configManager,
                          DataManager dataManager, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.bossBarManager = bossBarManager;
    }

    // ── Lifecycle ─────────────────────────────────────────────

    public void startScheduler() {
        if (!configManager.isContestEnabled()) return;
        Duration interval = configManager.getContestInterval();
        long intervalTicks = interval.toMillis() / 50;
        schedulerTask = new BukkitRunnable() {
            @Override public void run() {
                if (!contestActive) startContest();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    public void startContest() {
        if (contestActive) return;
        contestActive = true;
        contestStartTime = LocalDateTime.now();
        contestEndTime = contestStartTime.plus(configManager.getContestDuration());

        selectedCrops = selectRandomCrops();
        dataManager.clearContestData();

        // Lucky crop — chọn ngẫu nhiên 1 trong số selectedCrops
        if (configManager.isLuckyCropEnabled() && !selectedCrops.isEmpty()) {
            List<Material> cropList = new ArrayList<>(selectedCrops);
            luckyCrop = cropList.get(new Random().nextInt(cropList.size()));
        } else {
            luckyCrop = null;
        }

        // Broadcast start
        String cropsText = selectedCrops.stream()
                .map(configManager::getCropDisplayName)
                .collect(Collectors.joining(", "));
        broadcastMessage(configManager.getMessage("start").replace("{crops}", cropsText));

        // Title
        if (configManager.isTitleEnabled()) showContestTitle();

        // BossBar
        if (configManager.isBossBarEnabled()) bossBarManager.createContestBossBar(selectedCrops);

        // Thông báo lucky crop nếu bật
        if (luckyCrop != null && configManager.isLuckyCropAnnounceEnabled()) {
            String luckyMsg = configManager.getLuckyCropAnnounceMessage()
                    .replace("{crop}", configManager.getCropDisplayName(luckyCrop))
                    .replace("{multiplier}", formatMultiplier(configManager.getLuckyCropMultiplier()));
            broadcastMessage(luckyMsg);
        }

        startCountdownTask();
        String luckySuffix = luckyCrop != null ? " | Lucky crop: " + luckyCrop.name() : "";
        plugin.getLogger().info(configManager.getConsoleMessage("contest-started-log")
                .replace("{crops}", cropsText).replace("{lucky}", luckySuffix));
    }

    public void stopContest() {
        if (!contestActive) return;
        contestActive = false;

        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        bossBarManager.removeAllBossBars();

        distributeRewards();
        broadcastMessage(configManager.getMessage("end"));

        if (configManager.shouldResetPointsAfter()) dataManager.clearContestData();
        luckyCrop = null;
        plugin.getLogger().info(configManager.getConsoleMessage("contest-ended-log"));
    }

    // ── Crop selection ────────────────────────────────────────

    private Set<Material> selectRandomCrops() {
        Map<Material, Double> cropChances = configManager.getCropChances();
        int count = configManager.getCropSelectionCount();
        List<Material> available = new ArrayList<>(cropChances.keySet());
        Set<Material> selected = new HashSet<>();
        Random rng = new Random();

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            double total = available.stream().mapToDouble(cropChances::get).sum();
            double roll = rng.nextDouble() * total;
            double acc = 0;
            for (Material crop : available) {
                acc += cropChances.get(crop);
                if (roll <= acc) {
                    selected.add(crop);
                    available.remove(crop);
                    break;
                }
            }
        }
        return selected;
    }

    // ── Countdown task ────────────────────────────────────────

    private void startCountdownTask() {
        countdownTask = new BukkitRunnable() {
            @Override public void run() {
                if (!contestActive) { cancel(); return; }

                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(contestEndTime)) {
                    stopContest();
                    cancel();
                    return;
                }

                if (configManager.isBossBarEnabled())
                    bossBarManager.updateBossBar(selectedCrops, contestEndTime);

                if (configManager.isActionBarCountdownEnabled()) {
                    String timeLeft = formatTimeLeft(Duration.between(now, contestEndTime));
                    String text = configManager.getActionBarCountdownText().replace("{time}", timeLeft);
                    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
                    for (Player p : Bukkit.getOnlinePlayers()) p.sendActionBar(component);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ── Rewards & DB ─────────────────────────────────────────

    private void distributeRewards() {
        Map<UUID, Integer> leaderboard = dataManager.getLeaderboard();
        List<Map.Entry<UUID, Integer>> sorted = leaderboard.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        int minPoints = configManager.getMinParticipationPoints();
        DatabaseManager db = plugin.getDatabaseManager();

        // Top 1-3: thưởng + ghi DB
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Map.Entry<UUID, Integer> entry = sorted.get(i);
            if (entry.getValue() < minPoints) continue;

            int rank = i + 1;
            Player player = Bukkit.getPlayer(entry.getKey());
            String name = resolvePlayerName(entry.getKey(), player);

            db.recordContestResult(entry.getKey(), name, rank);

            if (player != null) {
                executeRewards(player, configManager.getRewards("top" + rank));
            }
        }

        // Participation (không trong top 3)
        Set<UUID> top3Uuids = sorted.subList(0, Math.min(3, sorted.size()))
                .stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        for (Map.Entry<UUID, Integer> entry : leaderboard.entrySet()) {
            if (top3Uuids.contains(entry.getKey())) continue;

            Player player = Bukkit.getPlayer(entry.getKey());
            String name = resolvePlayerName(entry.getKey(), player);

            if (entry.getValue() >= minPoints) {
                db.recordContestResult(entry.getKey(), name, 0);
                if (player != null) {
                    executeRewards(player, configManager.getRewards("participate"));
                    player.sendMessage(colorize(configManager.getMessage("participate-reward")
                            .replace("{points}", String.valueOf(entry.getValue()))));
                }
            } else {
                if (player != null) {
                    player.sendMessage(colorize(configManager.getMessage("not-enough")
                            .replace("{min-points}", String.valueOf(minPoints))));
                }
            }
        }
    }

    private void executeRewards(Player player, List<String> rewards) {
        for (String reward : rewards) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    reward.replace("{player}", player.getName()));
        }
    }

    // ── UI helpers ────────────────────────────────────────────

    private void showContestTitle() {
        Title title = Title.title(
                colorize(configManager.getTitleText()),
                colorize(configManager.getTitleSubtitle()),
                Title.Times.times(
                        Duration.ofMillis(configManager.getTitleFadeIn() * 50L),
                        Duration.ofMillis(configManager.getTitleStay() * 50L),
                        Duration.ofMillis(configManager.getTitleFadeOut() * 50L)
                )
        );
        for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(title);
    }

    private void broadcastMessage(String message) {
        Component component = colorize(message);
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(component);
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    private String formatTimeLeft(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String formatMultiplier(double multiplier) {
        if (multiplier == Math.floor(multiplier)) return String.valueOf((int) multiplier);
        return String.format("%.1f", multiplier);
    }

    /** Tìm tên player — ưu tiên online, fallback OfflinePlayer. */
    private String resolvePlayerName(UUID uuid, Player onlinePlayer) {
        if (onlinePlayer != null) return onlinePlayer.getName();
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : "Unknown";
    }

    // ── Shutdown ──────────────────────────────────────────────

    public void shutdown() {
        if (schedulerTask != null) schedulerTask.cancel();
        if (countdownTask != null)  countdownTask.cancel();
    }

    // ── Public helpers ────────────────────────────────────────

    public void sendContestInfo(Player player) {
        if (!contestActive) {
            player.sendMessage(colorize(configManager.getMessage("no-contest")));
            return;
        }
        String cropsText = selectedCrops.stream()
                .map(configManager::getCropDisplayName)
                .collect(Collectors.joining(", "));
        player.sendMessage(colorize(
                configManager.getAutoNotifyMessage().replace("{crops}", cropsText)));
    }

    public String getNextContestTime() {
        Duration timeUntilNext = configManager.getContestInterval();
        return formatDuration(timeUntilNext);
    }

    private String formatDuration(Duration duration) {
        long hours   = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        if (hours > 0)   return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %02ds", minutes, seconds);
        return String.format("%ds", seconds);
    }

    // ── Getters ───────────────────────────────────────────────

    public boolean isContestActive()         { return contestActive; }
    public Set<Material> getSelectedCrops()  { return selectedCrops; }
    public Material getLuckyCrop()           { return luckyCrop; }
    public LocalDateTime getContestEndTime() { return contestEndTime; }
}
