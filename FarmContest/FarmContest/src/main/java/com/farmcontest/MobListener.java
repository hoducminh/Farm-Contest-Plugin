package com.farmcontest;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MobListener implements Listener {

    private final FarmContest plugin;
    private final ContestManager contestManager;
    private final ConfigManager configManager;
    private final Random random = new Random();

    private final Map<UUID, long[]> killRateTracker = new ConcurrentHashMap<>();

    public MobListener(FarmContest plugin, ContestManager contestManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.contestManager = contestManager;
        this.configManager = configManager;
        startCleanupTask();
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                long windowMillis = configManager.getMobRateLimitWindowMillis();
                killRateTracker.entrySet().removeIf(e -> now - e.getValue()[0] > windowMillis);
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 30L, 20L * 30L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!contestManager.isContestActive()) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        EntityType type = event.getEntityType();
        if (!contestManager.getSelectedMobs().contains(type)) return;

        if (isRateLimited(killer.getUniqueId())) return;

        int basePoints = configManager.getMobPoints().getOrDefault(type, 0);
        if (basePoints <= 0) return;

        double finalPoints = basePoints;
        boolean isLuckyKill = false;
        double luckyMultiplier = 1.0;

        if (configManager.isLuckyMobEnabled()
                && type.equals(contestManager.getLuckyMob())
                && random.nextDouble() < configManager.getLuckyMobChance()) {
            isLuckyKill = true;
            luckyMultiplier = configManager.getLuckyMobMultiplier();
            finalPoints *= luckyMultiplier;
        }

        int points = (int) Math.round(finalPoints);

        contestManager.registerPointSource(killer.getUniqueId(), points);
        plugin.getDatabaseManager().incrementMobKillAsync(killer.getUniqueId(), killer.getName(), 1);

        if (configManager.isActionBarScoreEnabled()) {
            String msg;
            if (isLuckyKill) {
                msg = configManager.getLuckyMobMessage()
                        .replace("{points}", String.valueOf(points))
                        .replace("{multiplier}", formatMultiplier(luckyMultiplier))
                        .replace("{mob}", type.name());
            } else {
                msg = configManager.getActionBarScoreText()
                        .replace("{points}", String.valueOf(points))
                        .replace("{crop}", type.name());
            }
            killer.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        }
    }

    private String formatMultiplier(double multiplier) {
        if (multiplier == Math.floor(multiplier)) {
            return String.valueOf((int) multiplier);
        }
        return String.format("%.1f", multiplier);
    }

    private boolean isRateLimited(UUID uuid) {
        long now = System.currentTimeMillis();
        long windowMillis = configManager.getMobRateLimitWindowMillis();
        long[] state = killRateTracker.computeIfAbsent(uuid, k -> new long[]{now, 0});
        if (now - state[0] > windowMillis) {
            state[0] = now;
            state[1] = 0;
        }
        state[1]++;
        return state[1] > configManager.getMobRateLimitMaxKills();
    }
}
