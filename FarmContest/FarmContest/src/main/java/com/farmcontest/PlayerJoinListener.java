package com.farmcontest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    private final FarmContest plugin;
    private final ContestManager contestManager;
    private final ConfigManager configManager;
    public PlayerJoinListener(FarmContest plugin, ContestManager contestManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.contestManager = contestManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (contestManager.isContestActive()) {
            plugin.getBossBarManager().addPlayerBossBar(player, contestManager.getTargetsText(), contestManager.getContestEndTime());

            if (configManager.isAutoNotifyOnJoin()) {
                contestManager.sendContestInfo(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getBossBarManager().removePlayerBossBar(event.getPlayer());
    }
}