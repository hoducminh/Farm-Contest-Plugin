package com.farmcontest;

import com.farmcontest.contest.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ContestManager {
    private final FarmContest plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final BossBarManager bossBarManager;
    private final RewardDistributor rewardDistributor;

    private final ContestContext context = new ContestContext();
    private final Random contestTypeRandom = new Random();
    private ContestMode activeMode;
    private boolean contestActive = false;
    private BukkitTask schedulerTask;
    private BukkitTask countdownTask;
    private LocalDateTime nextContestAt;

    public ContestManager(FarmContest plugin, ConfigManager configManager,
                          DataManager dataManager, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.bossBarManager = bossBarManager;
        this.rewardDistributor = new RewardDistributor(configManager, dataManager, plugin.getDatabaseManager());
    }

    public RewardDistributor getRewardDistributor() { return rewardDistributor; }

    // ── Mode factory ──────────────────────────────────────────

    private ContestMode buildConfiguredMode() {
        if (configManager.isRandomContestSelectionEnabled()) {
            ContestType picked = pickRandomContestType();
            if (picked != null) return buildModeFromType(picked);
        }
        return buildFallbackMode();
    }

    private ContestMode buildFallbackMode() {
        ContestMode base = configManager.getContestModeType().equals("mob")
                ? new MobHuntMode(configManager, dataManager, rewardDistributor)
                : new FarmClassicMode(configManager, dataManager, rewardDistributor);

        return configManager.isContestCommunityScope()
                ? new CommunityMode(base, configManager, rewardDistributor)
                : base;
    }

    private ContestMode buildModeFromType(ContestType type) {
        ContestMode base = type.getModeType().equals("mob")
                ? new MobHuntMode(configManager, dataManager, rewardDistributor)
                : new FarmClassicMode(configManager, dataManager, rewardDistributor);

        return type.isCommunity()
                ? new CommunityMode(base, configManager, rewardDistributor)
                : base;
    }

    private ContestType pickRandomContestType() {
        Map<ContestType, Double> weights = configManager.getContestTypeWeights();
        List<ContestType> candidates = new ArrayList<>();
        double total = 0d;
        for (Map.Entry<ContestType, Double> entry : weights.entrySet()) {
            double weight = entry.getValue();
            if (weight < 0) continue;
            candidates.add(entry.getKey());
            total += weight;
        }
        if (candidates.isEmpty() || total <= 0d) return null;

        double roll = contestTypeRandom.nextDouble() * total;
        double cursor = 0d;
        for (ContestType type : candidates) {
            cursor += weights.get(type);
            if (roll < cursor) return type;
        }
        return candidates.get(candidates.size() - 1);
    }

    // ── Lifecycle ─────────────────────────────────────────────

    public void startScheduler() {
        if (!configManager.isContestEnabled()) return;
        Duration interval = configManager.getContestInterval();
        long intervalTicks = Math.max(20L, interval.toMillis() / 50);
        nextContestAt = LocalDateTime.now().plus(interval);
        schedulerTask = new BukkitRunnable() {
            @Override public void run() {
                if (!contestActive) {
                    startContest();
                    nextContestAt = LocalDateTime.now().plus(configManager.getContestInterval());
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    public void startContest() {
        startContest(buildConfiguredMode());
    }

    public void startContest(ContestMode mode) {
        if (contestActive) return;
        contestActive = true;
        activeMode = mode;
        context.reset();
        context.contestStartTime = LocalDateTime.now();
        context.contestEndTime = context.contestStartTime.plus(configManager.getContestDuration());

        activeMode.onStart(context);

        String targetsText = buildTargetsText();
        broadcastMessage(configManager.getMessage("start").replace("{crops}", targetsText));

        if (configManager.isTitleEnabled()) showContestTitle();
        if (configManager.isBossBarEnabled()) bossBarManager.createContestBossBar(targetsText);

        if (context.luckyCrop != null && configManager.isLuckyCropAnnounceEnabled()) {
            String luckyMsg = configManager.getLuckyCropAnnounceMessage()
                    .replace("{crop}", configManager.getCropDisplayName(context.luckyCrop))
                    .replace("{multiplier}", formatMultiplier(configManager.getLuckyCropMultiplier()));
            broadcastMessage(luckyMsg);
        }

        if (context.luckyMob != null && configManager.isLuckyMobAnnounceEnabled()) {
            String luckyMobMsg = configManager.getLuckyMobAnnounceMessage()
                    .replace("{mob}", context.luckyMob.name())
                    .replace("{multiplier}", formatMultiplier(configManager.getLuckyMobMultiplier()));
            broadcastMessage(luckyMobMsg);
        }

        startCountdownTask();
        plugin.getLogger().info(configManager.getConsoleMessage("contest-started-log")
                .replace("{crops}", targetsText).replace("{lucky}", ""));
    }

    public void stopContest() {
        if (!contestActive) return;
        contestActive = false;

        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        bossBarManager.removeAllBossBars();

        activeMode.onEnd(context);
        broadcastMessage(configManager.getMessage("end"));

        plugin.getLogger().info(configManager.getConsoleMessage("contest-ended-log"));
    }

    public void registerPointSource(UUID player, int points) {
        if (!contestActive || points <= 0) return;
        dataManager.addPoints(player, points);
        activeMode.onPointSource(context, player, points);
    }

    // ── Countdown task ────────────────────────────────────────

    private void startCountdownTask() {
        countdownTask = new BukkitRunnable() {
            @Override public void run() {
                if (!contestActive) { cancel(); return; }

                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(context.contestEndTime)) {
                    stopContest();
                    cancel();
                    return;
                }

                if (configManager.isBossBarEnabled())
                    bossBarManager.updateBossBar(buildTargetsText(), context.contestEndTime);

                if (configManager.isActionBarCountdownEnabled()) {
                    String timeLeft = formatTimeLeft(Duration.between(now, context.contestEndTime));
                    String text = configManager.getActionBarCountdownText().replace("{time}", timeLeft);
                    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
                    for (Player p : Bukkit.getOnlinePlayers()) p.sendActionBar(component);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ── UI helpers ────────────────────────────────────────────

    private String buildTargetsText() {
        if (!context.selectedCrops.isEmpty()) {
            return context.selectedCrops.stream()
                    .map(configManager::getCropDisplayName)
                    .collect(Collectors.joining(", "));
        }
        return context.selectedMobs.stream()
                .map(EntityType::name)
                .collect(Collectors.joining(", "));
    }

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
        player.sendMessage(colorize(
                configManager.getAutoNotifyMessage().replace("{crops}", buildTargetsText())));
    }

    public String getNextContestTime() {
        if (!configManager.isContestEnabled()) return configManager.getMessage("next-contest-disabled");
        if (contestActive) return configManager.getMessage("next-contest-running");
        if (nextContestAt == null) return formatDuration(configManager.getContestInterval());
        Duration remaining = Duration.between(LocalDateTime.now(), nextContestAt);
        if (remaining.isNegative()) remaining = Duration.ZERO;
        return formatDuration(remaining);
    }

    private String formatDuration(Duration duration) {
        long hours   = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        if (hours > 0)   return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %02ds", minutes, seconds);
        return String.format("%ds", seconds);
    }

    public String getTargetsText() { return buildTargetsText(); }

    // ── Getters ───────────────────────────────────────────────

    public boolean isContestActive()          { return contestActive; }
    public ContestContext getContext()        { return context; }
    public Set<Material> getSelectedCrops()   { return context.selectedCrops; }
    public Set<EntityType> getSelectedMobs()  { return context.selectedMobs; }
    public Material getLuckyCrop()            { return context.luckyCrop; }
    public EntityType getLuckyMob()            { return context.luckyMob; }
    public LocalDateTime getContestEndTime()  { return context.contestEndTime; }
    public ContestMode getActiveMode()        { return activeMode; }
}
