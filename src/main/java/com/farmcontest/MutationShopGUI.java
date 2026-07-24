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

public class MutationShopGUI implements Listener, InventoryHolder {

    private final FarmContest plugin;
    private final MutationManager mutationManager;
    private final Economy economy;

    public MutationShopGUI(FarmContest plugin, MutationManager mutationManager, Economy economy) {
        this.plugin = plugin;
        this.mutationManager = mutationManager;
        this.economy = economy;
    }

    /**
     * Mở GUI Shop cho người chơi dựa trên cấu hình số hàng (rows)
     */
    public void openShop(Player player) {
        int rows = mutationManager.getConfig().getInt("shop.rows", 3);
        if (rows < 1 || rows > 6) rows = 3; // Giới hạn từ 1 đến 6 hàng an toàn
        int size = rows * 9;

        String rawTitle = plugin.getConfigManager().getMutationShopMenuTitle();

        Component title = Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle));

        Inventory inv = Bukkit.createInventory(this, size, title);
        player.openInventory(inv);
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return null; 
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (!(event.getInventory().getHolder() instanceof MutationShopGUI)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inv = event.getInventory();
        double totalMoney = 0.0;
        boolean hasInvalidItems = false;


        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;


            if (mutationManager.isMutatedItem(item)) {

                double itemValue = mutationManager.getItemValue(item);
                totalMoney += itemValue * item.getAmount();

                inv.setItem(i, null);
            } else {
                hasInvalidItems = true;
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                inv.setItem(i, null);
            }
        }

        if (totalMoney > 0.0 && economy != null) {
            economy.depositPlayer(player, totalMoney);

            String rawMsg = plugin.getConfigManager().getMutationShopSuccessMessage();
            String msg = rawMsg.replace("{money}", String.format("%.2f", totalMoney));
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        }
        if (hasInvalidItems) {
            String rawReturnMsg = plugin.getConfigManager().getMutationShopInvalidItemMessage();
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(rawReturnMsg));
        }
    }
}