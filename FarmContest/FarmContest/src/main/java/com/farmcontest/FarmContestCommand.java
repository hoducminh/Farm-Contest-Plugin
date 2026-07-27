package com.farmcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class FarmContestCommand implements CommandExecutor {
    private final FarmContest plugin;
    private final ContestManager contestManager;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public FarmContestCommand(FarmContest plugin, ContestManager contestManager,
                              DataManager dataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.contestManager = contestManager;
        this.dataManager = dataManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { showHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "help"     -> showHelp(sender);
            case "start"    -> handleStart(sender);
            case "mode"     -> handleMode(sender, args);
            case "stop"     -> handleStop(sender);
            case "top"      -> handleTop(sender);
            case "info"     -> handleInfo(sender, args);
            case "profile"  -> handleProfile(sender, args);
            case "shop"     -> handleShop(sender); // Opens the mutated crop purchase shop
            case "mutation" -> handleMutation(sender, args); // 👈 Handles mutated crop commands
            case "autosell" -> handleAutosell(sender);
            case "add"      -> handlePointsModification(sender, args, "add");
            case "take"     -> handlePointsModification(sender, args, "take");
            case "set"      -> handlePointsModification(sender, args, "set");
            case "reset"    -> handleReset(sender);
            case "reload"   -> handleReload(sender);
            case "time"     -> handleTime(sender);
            default         -> showHelp(sender);
        }
        return true;
    }

    // ── Sub-commands ──────────────────────────────────────────

    private void showHelp(CommandSender sender) {
        for (String msg : configManager.getHelpCommands())
            sender.sendMessage(colorize(msg));

        send(sender, configManager.getHelpShopLine());
        if (configManager.isAutosellFeatureEnabled()) send(sender, configManager.getHelpAutosellLine());

        if (sender.hasPermission("farmcontest.admin")) {
            for (String msg : configManager.getHelpAdminCommands())
                sender.sendMessage(colorize(msg));
            send(sender, configManager.getHelpMutationLine());
        }
    }

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission("farmcontest.admin")) { noPermission(sender); return; }
        if (contestManager.isContestActive()) { send(sender, configManager.getMessage("contest-running")); return; }
        contestManager.startContest();
        send(sender, configManager.getMessage("contest-started"));
    }

    /** /fc mode <farm|mob> [community] — starts a contest with the specified mode */
    private void handleMode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("farmcontest.admin")) { noPermission(sender); return; }
        if (contestManager.isContestActive()) { send(sender, configManager.getMessage("contest-running")); return; }
        if (args.length < 2) { send(sender, configManager.getMessage("mode-usage")); return; }

        String type = args[1].toLowerCase();
        boolean community = args.length >= 3 && args[2].equalsIgnoreCase("community");

        var rewardDistributor = contestManager.getRewardDistributor();

        com.farmcontest.contest.ContestMode base = switch (type) {
            case "mob" -> new com.farmcontest.contest.MobHuntMode(configManager, dataManager, rewardDistributor);
            case "farm" -> new com.farmcontest.contest.FarmClassicMode(configManager, dataManager, rewardDistributor);
            default -> null;
        };

        if (base == null) { send(sender, configManager.getMessage("mode-usage")); return; }

        com.farmcontest.contest.ContestMode finalMode = community
                ? new com.farmcontest.contest.CommunityMode(base, configManager, rewardDistributor)
                : base;

        contestManager.startContest(finalMode);
        send(sender, configManager.getMessage("contest-started"));
    }

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission("farmcontest.admin")) { noPermission(sender); return; }
        if (!contestManager.isContestActive()) { send(sender, configManager.getMessage("no-contest")); return; }
        contestManager.stopContest();
        send(sender, configManager.getMessage("contest-stopped"));
    }

    private void handleTop(CommandSender sender) {
        if (!contestManager.isContestActive()) { send(sender, configManager.getMessage("no-contest")); return; }

        List<Map.Entry<UUID, Integer>> sorted = dataManager.getLeaderboard().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        send(sender, configManager.getMessage("top-header"));
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<UUID, Integer> e = sorted.get(i);
            Player p = Bukkit.getPlayer(e.getKey());
            String name = p != null ? p.getName() : "Unknown";
            String line = configManager.getMessage("top-entry")
                    .replace("{rank}", getRankPrefix(i + 1))
                    .replace("{player}", name)
                    .replace("{points}", String.valueOf(e.getValue()));
            send(sender, line);
        }
        if (sorted.isEmpty()) send(sender, configManager.getMessage("top-empty"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player p)) { send(sender, configManager.getMessage("player-only")); return; }
            int pts = dataManager.getPoints(p.getUniqueId());
            send(sender, configManager.getMessage("info-self").replace("{points}", String.valueOf(pts)));
        } else {
            if (!sender.hasPermission("farmcontest.info.other")) { noPermission(sender); return; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { send(sender, configManager.getMessage("invalid-player")); return; }
            String msg = configManager.getMessage("info-other")
                    .replace("{player}", target.getName())
                    .replace("{points}", String.valueOf(dataManager.getPoints(target.getUniqueId())));
            send(sender, msg);
        }
    }

    /** /fc profile [player] — views a farmer's profile from SQLite */
    private void handleProfile(CommandSender sender, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length == 1) {
            if (!(sender instanceof Player p)) { send(sender, configManager.getMessage("player-only")); return; }
            targetUuid = p.getUniqueId();
            targetName = p.getName();
        } else {
            if (!sender.hasPermission("farmcontest.profile.other")) { noPermission(sender); return; }
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            if (op.getName() == null) { send(sender, configManager.getMessage("invalid-player")); return; }
            targetUuid = op.getUniqueId();
            targetName = op.getName();
        }

        String finalTargetName = targetName;
        plugin.getDatabaseManager().getProfileAsync(targetUuid, profile -> {
            send(sender, configManager.getMessage("profile-header"));
            send(sender, configManager.getMessage("profile-name").replace("{player}", finalTargetName));
            send(sender, configManager.getMessage("profile-total-contests").replace("{value}", String.valueOf(profile.getTotalContests())));
            send(sender, configManager.getMessage("profile-top1").replace("{value}", String.valueOf(profile.getTop1Wins())));
            send(sender, configManager.getMessage("profile-top2").replace("{value}", String.valueOf(profile.getTop2Wins())));
            send(sender, configManager.getMessage("profile-top3").replace("{value}", String.valueOf(profile.getTop3Wins())));
            send(sender, configManager.getMessage("profile-harvests").replace("{value}", String.valueOf(profile.getTotalHarvests())));
            send(sender, configManager.getMessage("profile-mob-kills").replace("{value}", String.valueOf(profile.getTotalMobKills())));
            if (configManager.isMedalSystemEnabled()) {
                for (var tier : configManager.getMedalTiers()) {
                    send(sender, configManager.getMessage("profile-medal-line")
                            .replace("{medal}", tier.display())
                            .replace("{count}", String.valueOf(profile.getMedalCount(tier.id()))));
                }
            }
            send(sender, configManager.getMessage("profile-footer"));
        });
    }

    /** /fc autosell — toggles auto-selling mutated crops on harvest */
    private void handleAutosell(CommandSender sender) {
        if (!(sender instanceof Player player)) { send(sender, configManager.getMessage("player-only")); return; }
        if (!player.hasPermission("farmcontest.autosell")) { noPermission(sender); return; }
        if (!configManager.isAutosellFeatureEnabled()) { send(sender, configManager.getMessage("autosell-feature-disabled")); return; }
        if (plugin.getEconomy() == null) { send(sender, configManager.getMessage("shop-vault-disabled")); return; }

        boolean newState = !plugin.getMutationManager().isAutosellEnabled(player);
        plugin.getMutationManager().setAutosellEnabled(player, newState);
        send(sender, configManager.getMessage(newState ? "autosell-enabled" : "autosell-disabled"));
    }

    /** /fc shop — opens the mutated crop purchase GUI */
    private void handleShop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, configManager.getMessage("shop-player-only"));
            return;
        }

        if (plugin.getEconomy() == null) {
            send(player, configManager.getMessage("shop-vault-disabled"));
            return;
        }

        plugin.getMutationShopGUI().openShop(player);
    }

    /** /fc mutation give <player> <crop> <mutation> [weight_min] [weight_max] */
    private void handleMutation(CommandSender sender, String[] args) {
        if (!sender.hasPermission("farmcontest.admin")) { noPermission(sender); return; }

        if (args.length < 5 || !args[1].equalsIgnoreCase("give")) {
            send(sender, configManager.getMessage("mutation-usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) { send(sender, configManager.getMessage("invalid-player")); return; }

        Material cropMaterial;
        try {
            cropMaterial = Material.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            send(sender, configManager.getMessage("invalid-crop"));
            return;
        }

        String mutationKey = args[4];
        if (plugin.getMutationManager().getConfig().getConfigurationSection("mutations." + mutationKey) == null) {
            send(sender, configManager.getMessage("invalid-mutation").replace("{mutation}", mutationKey));
            return;
        }


        ItemStack item = plugin.getMutationManager().createMutatedItemStack(target, cropMaterial, mutationKey);


        if (args.length >= 6) {
            try {
                double minW = Double.parseDouble(args[5]);
                double maxW = args.length >= 7 ? Double.parseDouble(args[6]) : minW;
                double customWeight = Math.round((minW + (maxW - minW) * new Random().nextDouble()) * 100.0) / 100.0;

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    NamespacedKey keyWeight = new NamespacedKey(plugin, "mutation_weight");
                    meta.getPersistentDataContainer().set(keyWeight, PersistentDataType.DOUBLE, customWeight);
                    var mutationConfig = plugin.getMutationManager().getConfig();
                    String defaultName = configManager.getMutationDefaultName();
                    String defaultDisplay = configManager.getMutationDefaultDisplay();
                    String mutationName = mutationConfig.getString("mutations." + mutationKey + ".name", defaultName);
                    String mutationDisplay = mutationConfig.getString("mutations." + mutationKey + ".display", defaultDisplay);
                    double multiplier = mutationConfig.getDouble("mutations." + mutationKey + ".multiplier", 1.5);

                    List<String> rawLore = configManager.getMutationItemLore();
                    List<Component> formattedLore = new ArrayList<>();
                    for (String line : rawLore) {
                        String processed = line.replace("{mutation_name}", mutationName)
                                .replace("{mutation_display}", mutationDisplay)
                                .replace("{crop_display}", args[3])
                                .replace("{multiplier}", String.valueOf(multiplier))
                                .replace("{weight}", String.valueOf(customWeight))
                                .replace("{owner}", target.getName());
                        formattedLore.add(parseColor(processed));
                    }

                    meta.lore(formattedLore);
                    item.setItemMeta(meta);
                }
            } catch (NumberFormatException ignored) {}
        }

        target.getInventory().addItem(item);
        String given = configManager.getMessage("mutation-given")
                .replace("{mutation}", mutationKey)
                .replace("{player}", target.getName());
        send(sender, given);
    }

    private void handleTime(CommandSender sender) {
        String time;
        if (!configManager.isContestEnabled()) {
            send(sender, configManager.getMessage("next-contest-disabled"));
            return;
        }
        if (contestManager.isContestActive()) {
            send(sender, configManager.getMessage("next-contest-running"));
            return;
        }
        time = contestManager.getNextContestTime();
        String msg = configManager.getMessage("next-contest").replace("{time}", time);
        send(sender, msg);
    }

    private void handlePointsModification(CommandSender sender, String[] args, String action) {
        if (!sender.hasPermission("farmcontest.admin")) { noPermission(sender); return; }
        if (!contestManager.isContestActive()) { send(sender, configManager.getMessage("no-contest-for-points")); return; }
        if (args.length < 3) {
            send(sender, configManager.getMessage("points-usage").replace("{action}", action));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { send(sender, configManager.getMessage("invalid-player")); return; }

        int points;
        try { points = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) { send(sender, configManager.getMessage("invalid-points")); return; }

        boolean silent = args.length > 3 && args[3].equalsIgnoreCase("silent:true");

        switch (action) {
            case "add" -> {
                dataManager.addPoints(target.getUniqueId(), points);
                if (!silent) send(target, configManager.getMessage("add-point").replace("{points}", String.valueOf(points)));
            }
            case "take" -> {
                dataManager.removePoints(target.getUniqueId(), points);
                if (!silent) send(target, configManager.getMessage("take-point").replace("{points}", String.valueOf(points)));
            }
            case "set" -> {
                dataManager.setPoints(target.getUniqueId(), points);
                if (!silent) send(target, configManager.getMessage("set-point").replace("{points}", String.valueOf(points)));
            }
        }
        String adminMsg = configManager.getMessage("admin-points-modified")
                .replace("{action}", action)
                .replace("{points}", String.valueOf(points))
                .replace("{player}", target.getName());
        send(sender, adminMsg);
    }

    private void handleReset(CommandSender sender) {
        if (!sender.hasPermission("farmcontest.admin")) { noPermission(sender); return; }
        dataManager.clearContestData();
        send(sender, configManager.getMessage("points-reset"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("farmcontest.admin")) { noPermission(sender); return; }

        configManager.reload();

        if (plugin.getMutationManager() != null) {
            plugin.getMutationManager().loadConfig();
        }

        send(sender, configManager.getMessage("reload-success"));
    }

    // ── Helpers ───────────────────────────────────────────────

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(colorize(msg));
    }

    private void noPermission(CommandSender sender) {
        send(sender, configManager.getMessage("no-permission"));
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
    private Component parseColor(String legacyAmpersandText) {
        String text = legacyAmpersandText == null ? "" : legacyAmpersandText;
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }

    private String getRankPrefix(int rank) {
        return switch (rank) {
            case 1 -> configManager.getMessage("rank-1");
            case 2 -> configManager.getMessage("rank-2");
            case 3 -> configManager.getMessage("rank-3");
            default -> configManager.getMessage("rank-other").replace("{rank}", String.valueOf(rank));
        };
    }
}
