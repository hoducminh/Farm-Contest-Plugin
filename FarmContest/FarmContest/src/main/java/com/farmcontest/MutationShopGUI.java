package com.farmcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * GUI for purchasing mutated crops from players.
 *
 * <p>Compat 1.21.8–1.21.11: uses {@link InventoryCloseEvent#getReason()} (available since
 * Paper 1.20.4, stable in 1.21.x) to skip processing when the server/plugin force-closes
 * the inventory — preventing item loss or double-processing bugs on reload/shutdown.
 */
public class MutationShopGUI implements Listener, InventoryHolder {

    private final FarmContest plugin;
    private final MutationManager mutationManager;
    private final Economy economy;

    /** Close reasons that do NOT originate from the player → no sale processing. */
    private static final java.util.Set<InventoryCloseEvent.Reason> FORCE_CLOSE_REASONS =
            java.util.EnumSet.of(
                InventoryCloseEvent.Reason.PLUGIN,          // Plugin force-close
                InventoryCloseEvent.Reason.UNLOADED,        // Chunk/world unloaded
                InventoryCloseEvent.Reason.CANT_USE         // Server cancelled inventory
            );

    public MutationShopGUI(FarmContest plugin, MutationManager mutationManager, Economy economy) {
        this.plugin = plugin;
        this.mutationManager = mutationManager;
        this.economy = economy;
    }

    // ── Open ──────────────────────────────────────────────────

    /**
     * Opens the shop GUI for a player. Row count is read from mutation.yml → {@code shop.rows}.
     */
    public void openShop(Player player) {
        int rows = mutationManager.getConfig().getInt("shop.rows", 3);
        if (rows < 1 || rows > 6) rows = 3;

        String rawTitle = plugin.getConfigManager().getMutationShopMenuTitle();
        Component title = Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle));

        Inventory inv = Bukkit.createInventory(this, rows * 9, title);
        player.openInventory(inv);
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        // InventoryHolder placeholder — GUI does not need its own backing inventory.
        return Bukkit.createInventory(this, 9);
    }

    // ── Close handler ─────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MutationShopGUI)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // 1.21.x compat: skip processing if the server/plugin force-closed the GUI — return items
        InventoryCloseEvent.Reason reason = event.getReason();
        if (FORCE_CLOSE_REASONS.contains(reason)) {
            returnItemsToPlayer(event.getInventory(), player);
            return;
        }

        // Player closed the GUI themselves → process the sale
        processSale(event.getInventory(), player);
    }

    // ── Private helpers ───────────────────────────────────────

    /**
     * Returns all items in the inventory to the player (or drops them if the inventory is full).
     * Called when the server/plugin force-closes the GUI — no money is converted.
     */
    private void returnItemsToPlayer(Inventory inv, Player player) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            inv.setItem(i, null);
        }
    }

    /**
     * Evaluates each item in the GUI:
     * — valid mutated crop → converted into money.
     * — any other item → returned to the player's inventory.
     */
    private void processSale(Inventory inv, Player player) {
        double totalMoney   = 0.0;
        boolean hasInvalid  = false;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;

            if (mutationManager.isMutatedItem(item)) {
                totalMoney += mutationManager.getItemValue(item) * item.getAmount();
                inv.setItem(i, null);
            } else {
                hasInvalid = true;
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                inv.setItem(i, null);
            }
        }

        if (totalMoney > 0.0 && economy != null) {
            economy.depositPlayer(player, totalMoney);
            String msg = plugin.getConfigManager().getMutationShopSuccessMessage()
                    .replace("{money}", String.format("%.2f", totalMoney));
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        }

        if (hasInvalid) {
            String msg = plugin.getConfigManager().getMutationShopInvalidItemMessage();
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        }
    }
}
