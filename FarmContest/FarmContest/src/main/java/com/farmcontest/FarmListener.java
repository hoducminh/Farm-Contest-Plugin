package com.farmcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class FarmListener implements Listener {

    private final FarmContest plugin;
    private final ContestManager contestManager;
    private final ConfigManager configManager;
    private final Random random = new Random();

    private static final long TRACK_TTL_MILLIS = 30L * 60L * 1000L;
    private final Map<Location, Long> playerPlacedTracker = new ConcurrentHashMap<>();
    private final Map<Location, Long> naturallyGrownTracker = new ConcurrentHashMap<>();

    public FarmListener(FarmContest plugin, ContestManager contestManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.contestManager = contestManager;
        this.configManager = configManager;
        startCleanupTask();
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                playerPlacedTracker.entrySet().removeIf(e -> now - e.getValue() > TRACK_TTL_MILLIS);
                naturallyGrownTracker.entrySet().removeIf(e -> now - e.getValue() > TRACK_TTL_MILLIS);
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60L * 5L, 20L * 60L * 5L);
    }

    private boolean isPlayerPlaced(Block block) {
        return playerPlacedTracker.containsKey(block.getLocation());
    }

    private boolean isNaturallyGrown(Block block) {
        return naturallyGrownTracker.containsKey(block.getLocation());
    }

    private void markPlayerPlaced(Block block) {
        playerPlacedTracker.put(block.getLocation(), System.currentTimeMillis());
    }

    private void markNaturallyGrown(Block block) {
        naturallyGrownTracker.put(block.getLocation(), System.currentTimeMillis());
    }

    private void clearTracking(Block block) {
        Location loc = block.getLocation();
        playerPlacedTracker.remove(loc);
        naturallyGrownTracker.remove(loc);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlock();
        Material placedType = placedBlock.getType();

        if (isTrackedCrop(placedType)) {
            markPlayerPlaced(placedBlock);
        }
        naturallyGrownTracker.remove(placedBlock.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Material grownType = event.getNewState().getType();
        if (grownType == Material.SUGAR_CANE || grownType == Material.CACTUS) {
            Block grownBlock = event.getBlock();
            markNaturallyGrown(grownBlock);
            playerPlacedTracker.remove(grownBlock.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!contestManager.isContestActive()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material broken = block.getType();


        if (!contestManager.getSelectedCrops().contains(broken)) return;


        if (broken == Material.SUGAR_CANE || broken == Material.CACTUS) {
            handleTallCropBreak(player, block, broken);
            return;
        }


        if (broken == Material.PUMPKIN || broken == Material.MELON) {
            if (isPlayerPlaced(block)) {
                playerPlacedTracker.remove(block.getLocation());
                return;
            }
        }


        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) {
                return;
            }
        }


        processCropReward(player, block, broken, 1);
    }

    private void handleTallCropBreak(Player player, Block brokenBlock, Material material) {
        List<Block> column = new ArrayList<>();
        List<Boolean> validFlags = new ArrayList<>();

        Block current = brokenBlock;
        while (current.getType() == material) {
            boolean isNatural = isNaturallyGrown(current);
            boolean isPlayerPlaced = isPlayerPlaced(current);

            column.add(current);
            validFlags.add(isNatural && !isPlayerPlaced);

            current = current.getRelative(BlockFace.UP);
        }


        int validHarvestCount = 0;
        for (boolean valid : validFlags) {
            if (valid) validHarvestCount++;
        }


        for (Block b : column) {
            clearTracking(b);
        }

        if (validHarvestCount <= 0) return;

        processCropReward(player, brokenBlock, material, validHarvestCount);
    }
    private void processCropReward(Player player, Block block, Material broken, int count) {
        Map<Material, Integer> cropPoints = configManager.getCropPoints();
        int basePoints = cropPoints.getOrDefault(broken, 0) * count;
        if (basePoints <= 0) return;

        double finalPoints = basePoints;
        String actionBarMessage = null;
        boolean weatherBoosted = false;
        double weatherMultiplier = 1.0;

        if (configManager.isWeatherBuffEnabled()) {
            boolean isRaining = player.getWorld().hasStorm();
            long time = player.getWorld().getTime();

            if (isRaining && configManager.getRainBoostCrops().contains(broken)) {
                weatherMultiplier = configManager.getRainBoostMultiplier();
                finalPoints *= weatherMultiplier;
                weatherBoosted = true;
                actionBarMessage = configManager.getRainBoostMessage();
            } else if (!isRaining && time >= configManager.getSunBoostTimeStart() && time <= configManager.getSunBoostTimeEnd() && configManager.getSunBoostCrops().contains(broken)) {
                weatherMultiplier = configManager.getSunBoostMultiplier();
                finalPoints *= weatherMultiplier;
                weatherBoosted = true;
                actionBarMessage = configManager.getSunBoostMessage();
            }
        }

        // Lucky Crop
        boolean isLuckyHarvest = false;
        double luckyMultiplier = 1.0;

        if (configManager.isLuckyCropEnabled()
                && broken.equals(contestManager.getLuckyCrop())
                && random.nextDouble() < configManager.getLuckyCropChance()) {

            isLuckyHarvest = true;
            luckyMultiplier = configManager.getLuckyCropMultiplier();
            finalPoints *= luckyMultiplier;
            dropLuckyItem(block.getLocation());
        }

        if (plugin.getMutationManager() != null) {
            plugin.getMutationManager().tryCreateMutation(player, block, broken);
        }

        int earnedPoints = (int) Math.round(finalPoints);
        contestManager.registerPointSource(player.getUniqueId(), earnedPoints);

        plugin.getDatabaseManager().incrementHarvestAsync(player.getUniqueId(), player.getName(), count);

        //ActionBar
        if (configManager.isActionBarScoreEnabled()) {
            String cropName = configManager.getCropDisplayName(broken);
            String msg;

            if (isLuckyHarvest) {
                msg = configManager.getLuckyCropMessage()
                        .replace("{points}", String.valueOf(earnedPoints))
                        .replace("{multiplier}", formatMultiplier(luckyMultiplier))
                        .replace("{crop}", cropName);
            } else if (weatherBoosted && actionBarMessage != null) {
                msg = actionBarMessage
                        .replace("{points}", String.valueOf(earnedPoints))
                        .replace("{multiplier}", formatMultiplier(weatherMultiplier))
                        .replace("{crop}", cropName);
            } else {
                msg = configManager.getActionBarScoreText()
                        .replace("{points}", String.valueOf(earnedPoints))
                        .replace("{crop}", cropName);
            }

            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        }
    }

    private boolean isTrackedCrop(Material material) {
        return material == Material.PUMPKIN || material == Material.MELON
                || material == Material.CARVED_PUMPKIN || material == Material.SUGAR_CANE
                || material == Material.CACTUS;
    }
    private void dropLuckyItem(Location location) {
        Material mat = configManager.getLuckyCropDropItem();
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(parseColor(configManager.getLuckyCropItemName()));

            List<Component> lore = new ArrayList<>();
            for (String line : configManager.getLuckyCropItemLore()) {
                lore.add(parseColor(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        location.getWorld().dropItemNaturally(location, item);
    }

    private Component parseColor(String legacyAmpersandText) {
        String text = legacyAmpersandText == null ? "" : legacyAmpersandText;
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }

    private String formatMultiplier(double multiplier) {
        if (multiplier == Math.floor(multiplier)) {
            return String.valueOf((int) multiplier);
        }
        return String.format("%.1f", multiplier);
    }
}