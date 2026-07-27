package com.farmcontest;

import com.farmcontest.contest.CommunityMilestone;
import com.farmcontest.contest.ContestType;
import com.farmcontest.contest.MedalTier;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.time.Duration;
import java.util.*;

public class ConfigManager {
    private static final Duration MIN_DURATION = Duration.ofSeconds(10);
    private final FarmContest plugin;
    private FileConfiguration config;
    private List<CommunityMilestone> cachedMilestones = List.of();
    private List<MedalTier> cachedMedalTiers = List.of();

    public ConfigManager(FarmContest plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        cachedMilestones = parseCommunityMilestones();
        cachedMedalTiers = parseMedalTiers();
    }

    // ── Contest core ──────────────────────────────────────────
    public boolean isContestEnabled() {
        return config.getBoolean("contest.enabled", true);
    }

    public Duration getContestInterval() {
        return clampMin(parseDuration(config.getString("contest.interval", "3h")));
    }

    public Duration getContestDuration() {
        return clampMin(parseDuration(config.getString("contest.duration", "15m")));
    }

    public int getMinParticipationPoints() {
        return config.getInt("contest.min-participation-points", 200);
    }

    public int getCropSelectionCount() {
        return config.getInt("contest.crop-selection.count", 3);
    }

    public Map<Material, Double> getCropChances() {
        Map<Material, Double> chances = new HashMap<>();
        var section = config.getConfigurationSection("contest.crop-selection.chance_crop");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    chances.put(material, section.getDouble(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(getConsoleMessage("invalid-crop-warn").replace("{key}", key));
                }
            }
        }
        return chances;
    }

    public Map<Material, Integer> getCropPoints() {
        Map<Material, Integer> points = new HashMap<>();
        var section = config.getConfigurationSection("crop_point");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    points.put(material, section.getInt(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(getConsoleMessage("invalid-crop-warn").replace("{key}", key));
                }
            }
        }
        return points;
    }

    public int getMobSelectionCount() {
        return config.getInt("contest.mob-selection.count", 3);
    }

    public long getMobRateLimitWindowMillis() {
        return config.getLong("contest.mob-selection.mob-rate-limit.window-millis", 1000L);
    }

    public int getMobRateLimitMaxKills() {
        return config.getInt("contest.mob-selection.mob-rate-limit.max-kills-per-window", 10);
    }

    public Map<EntityType, Double> getMobChances() {
        Map<EntityType, Double> chances = new HashMap<>();
        var section = config.getConfigurationSection("contest.mob-selection.chance_mob");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    chances.put(type, section.getDouble(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(getConsoleMessage("invalid-mob-warn").replace("{key}", key));
                }
            }
        }
        if (chances.isEmpty()) {
            for (EntityType type : getMobPoints().keySet()) {
                chances.put(type, 1.0);
            }
        }
        return chances;
    }

    public String getContestModeType() {
        return config.getString("contest.mode", "farm").toLowerCase();
    }

    public boolean isContestCommunityScope() {
        return config.getBoolean("contest.community-scope", false);
    }

    public boolean isRandomContestSelectionEnabled() {
        return config.getBoolean("contest.random-selection.enabled", false);
    }

    public double getContestTypeWeight(ContestType type) {
        return config.getDouble("contest.random-selection.weights." + type.getKey(), 1.0);
    }

    public Map<ContestType, Double> getContestTypeWeights() {
        Map<ContestType, Double> weights = new EnumMap<>(ContestType.class);
        for (ContestType type : ContestType.values()) {
            weights.put(type, getContestTypeWeight(type));
        }
        return weights;
    }

    public Map<EntityType, Integer> getMobPoints() {
        Map<EntityType, Integer> points = new HashMap<>();
        var section = config.getConfigurationSection("mob_point");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    points.put(type, section.getInt(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(getConsoleMessage("invalid-mob-warn").replace("{key}", key));
                }
            }
        }
        return points;
    }

    public List<CommunityMilestone> getCommunityMilestones() {
        return cachedMilestones;
    }

    private List<CommunityMilestone> parseCommunityMilestones() {
        List<CommunityMilestone> result = new ArrayList<>();
        List<Map<?, ?>> raw = config.getMapList("community.milestones");
        for (Map<?, ?> entry : raw) {
            Object rawTarget = entry.get("target");
            long target = rawTarget instanceof Number n ? n.longValue() : 0L;
            List<String> commands = new ArrayList<>();

            // New key: "reward" (supports list)
            Object reward = entry.get("reward");
            if (reward instanceof String s) {
                commands.add(s);
            } else if (reward instanceof List<?> list) {
                list.forEach(o -> commands.add(String.valueOf(o)));
            } else {
                // Legacy fallback: "reward-command" (single string or list)
                Object legacy = entry.get("reward-command");
                if (legacy instanceof String s) commands.add(s);
                else if (legacy instanceof List<?> list) list.forEach(o -> commands.add(String.valueOf(o)));
            }

            result.add(new CommunityMilestone(target, commands));
        }
        result.sort(Comparator.comparingLong(CommunityMilestone::target));
        return result;
    }

    public List<MedalTier> getMedalTiers() {
        return cachedMedalTiers;
    }

    private List<MedalTier> parseMedalTiers() {
        List<MedalTier> tiers = new ArrayList<>();
        List<Map<?, ?>> raw = config.getMapList("medal-system.tiers");
        if (!raw.isEmpty()) {
            for (Map<?, ?> entry : raw) {
                String id = String.valueOf(entry.get("id"));
                Object rawThreshold = entry.get("threshold");
                int threshold = rawThreshold instanceof Number n ? n.intValue() : 0;
                Object rawDisplay = entry.get("display");
                String display = rawDisplay != null ? String.valueOf(rawDisplay) : id;
                tiers.add(new MedalTier(id, threshold, display));
            }
        } else {
            ConfigurationSection legacy = config.getConfigurationSection("medal-system.thresholds");
            if (legacy != null) {
                for (String key : legacy.getKeys(false)) {
                    tiers.add(new MedalTier(key, legacy.getInt(key), getMedalDisplayName(key)));
                }
            }
        }
        tiers.sort((a, b) -> Integer.compare(b.threshold(), a.threshold()));
        return tiers;
    }

    // ── Notify ────────────────────────────────────────────────
    public boolean isAutoNotifyOnJoin() {
        return config.getBoolean("auto-notify-on-join", true);
    }

    public String getAutoNotifyMessage() {
        return config.getString("auto-notify-message", "&6Cuộc thi đang diễn ra!");
    }

    // ── BossBar ───────────────────────────────────────────────
    public boolean isBossBarEnabled() {
        return config.getBoolean("display_contest.bossbar.enabled", true);
    }

    public String getBossBarText() {
        return config.getString("display_contest.bossbar.text", "&aCuộc thi nông sản!");
    }

    public BarColor getBossBarColor() {
        try {
            return BarColor.valueOf(config.getString("display_contest.bossbar.color", "GREEN"));
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }

    public BarStyle getBossBarStyle() {
        try {
            return BarStyle.valueOf(config.getString("display_contest.bossbar.style", "SEGMENTED_10"));
        } catch (IllegalArgumentException e) {
            return BarStyle.SEGMENTED_10;
        }
    }

    // ── Title ─────────────────────────────────────────────────
    public boolean isTitleEnabled() {
        return config.getBoolean("display_contest.title.start.enabled", true);
    }

    public String getTitleText() {
        return config.getString("display_contest.title.start.text", "&eFARM CONTEST");
    }

    public String getTitleSubtitle() {
        return config.getString("display_contest.title.start.subtitle", "&fFarm selected crops!");
    }

    public int getTitleFadeIn()  { return config.getInt("display_contest.title.start.fadeIn",  20); }
    public int getTitleStay()    { return config.getInt("display_contest.title.start.stay",     60); }
    public int getTitleFadeOut() { return config.getInt("display_contest.title.start.fadeOut",  20); }

    // ── ActionBar ─────────────────────────────────────────────
    public boolean isActionBarScoreEnabled() {
        return config.getBoolean("display_contest.actionbar.score.enabled", true);
    }

    public String getActionBarScoreText() {
        return config.getString("display_contest.actionbar.score.text", "&6+{points} points &7({crop})");
    }

    public boolean isActionBarCountdownEnabled() {
        return config.getBoolean("display_contest.actionbar.countdown.enabled", true);
    }

    public String getActionBarCountdownText() {
        return config.getString("display_contest.actionbar.countdown.text", "&aTime remaining: &f{time}");
    }

    // ── Messages / Rewards / Help ─────────────────────────────
    public String getMessage(String key) {
        return config.getString("messages." + key, "&cMessage not found: " + key);
    }

    public List<String> getRewards(String category) {
        return config.getStringList("rewards." + category);
    }

    public List<String> getHelpCommands() {
        return config.getStringList("commands.help");
    }

    public List<String> getHelpAdminCommands() {
        return config.getStringList("help-admin");
    }

    public String getHelpShopLine() {
        return config.getString("help-shop-line", "&e/fc shop");
    }

    public String getHelpMutationLine() {
        return config.getString("help-mutation-line", "&e/fc mutation give <player> <crop> <mutation>");
    }

    public String getHelpAutosellLine() {
        return config.getString("help-autosell-line", "&e/fc autosell &7- Bật/tắt tự động bán nông sản đột biến");
    }

    // ── Console log messages ──────────────────────────────────
    public String getConsoleMessage(String key) {
        return config.getString("console-messages." + key, key);
    }

    // ── Mutation messages (shared, separated from mutation.yml) ─
    public String getMutationDefaultName() {
        return config.getString("mutation-messages.default-mutation-name", "Đột Biến");
    }

    public String getMutationDefaultDisplay() {
        return config.getString("mutation-messages.default-mutation-display", "Đặc Biệt");
    }

    public String getMutationBroadcastMessage() {
        return config.getString("mutation-messages.broadcast-message",
                "&6✨ Bạn đã thu hoạch được &e{mutation} {crop}&6 với hệ số &ex{multiplier}&6!");
    }

    public String getMutationItemNameFormat() {
        return config.getString("mutation-messages.item-format.name", "&6&l{mutation_name} {crop_display}");
    }

    public List<String> getMutationItemLore() {
        return config.getStringList("mutation-messages.item-format.lore");
    }

    public String getMutationShopMenuTitle() {
        return config.getString("mutation-messages.shop.menu-title", "&8[GUI] Thu Mua Nông Sản Đột Biến");
    }

    public String getMutationShopSuccessMessage() {
        return config.getString("mutation-messages.shop.success-message",
                "&aĐã quy đổi thành công và nhận được &e${money}&a!");
    }

    public String getMutationShopInvalidItemMessage() {
        return config.getString("mutation-messages.shop.invalid-item-returned",
                "&cCó item không hợp lệ đã được trả lại vào túi đồ của bạn.");
    }

    // ── Medal system ──────────────────────────────────────────
    public boolean isMedalSystemEnabled() {
        return config.getBoolean("medal-system.enabled", true);
    }

    public String getMedalAwardMessage() {
        return config.getString("medal-system.message",
                "&6✨ Bạn đã nhận huy chương &e{medal} &6với &e{points} điểm&6!");
    }

    public String getMedalDisplayName(String tier) {
        return config.getString("medal-system.display." + tier, tier);
    }

    // ── Autosell ──────────────────────────────────────────────
    public boolean isAutosellFeatureEnabled() {
        return config.getBoolean("mutation-messages.autosell.enabled", true);
    }

    public String getAutosellSoldMessage() {
        return config.getString("mutation-messages.autosell.sold-message",
                "&a✨ Tự động bán nông sản đột biến &7({crop}) &a+ &e${money}&a!");
    }

    // ── Data ──────────────────────────────────────────────────
    public Duration getSaveInterval() {
        return clampMin(parseDuration(config.getString("data.save-interval", "5m")));
    }

    public boolean shouldResetPointsAfter() {
        return config.getBoolean("data.reset-points-after", true);
    }

    // ── Crop display names ────────────────────────────────────
    public Map<Material, String> getCropPlaceholders() {
        Map<Material, String> placeholders = new HashMap<>();
        var section = config.getConfigurationSection("crop_placeholder");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    placeholders.put(material, section.getString(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(getConsoleMessage("invalid-crop-warn").replace("{key}", key));
                }
            }
        }
        return placeholders;
    }

    public String getCropDisplayName(Material crop) {
        return getCropPlaceholders().getOrDefault(
                crop, crop.name().toLowerCase().replace("_", " "));
    }

    // ── Lucky Crop ────────────────────────────────────────────
    public boolean isLuckyCropEnabled() {
        return config.getBoolean("event_modifiers.lucky_crop.enabled", true);
    }

    public double getLuckyCropChance() {
        return config.getDouble("event_modifiers.lucky_crop.chance", 0.05);
    }

    public double getLuckyCropMultiplier() {
        return config.getDouble("event_modifiers.lucky_crop.multiplier", 2.0);
    }

    public Material getLuckyCropDropItem() {
        String raw = config.getString("event_modifiers.lucky_crop.drop_item", "GOLDEN_CARROT");
        try {
            return Material.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.GOLDEN_CARROT;
        }
    }

    public String getLuckyCropItemName() {
        return config.getString("event_modifiers.lucky_crop.item_name", "&6✨ Nông Sản Hoàng Kim");
    }

    public List<String> getLuckyCropItemLore() {
        return config.getStringList("event_modifiers.lucky_crop.item_lore");
    }

    public String getLuckyCropMessage() {
        return config.getString("event_modifiers.lucky_crop.message",
                "&6✨ Nông Sản Hoàng Kim! &a+{points} điểm &7(x{multiplier}) &e- {crop}");
    }

    public boolean isLuckyCropAnnounceEnabled() {
        return config.getBoolean("event_modifiers.lucky_crop.announce_lucky_crop", true);
    }

    public String getLuckyCropAnnounceMessage() {
        return config.getString("event_modifiers.lucky_crop.announce_message",
                "&6✨ Nông Sản May Mắn: &f{crop}&6! Thu hoạch để có cơ hội x{multiplier} điểm!");
    }

    // ── Lucky Mob ─────────────────────────────────────────────
    public boolean isLuckyMobEnabled() {
        return config.getBoolean("event_modifiers.lucky_mob.enabled", true);
    }

    public double getLuckyMobChance() {
        return config.getDouble("event_modifiers.lucky_mob.chance", 0.05);
    }

    public double getLuckyMobMultiplier() {
        return config.getDouble("event_modifiers.lucky_mob.multiplier", 2.0);
    }

    public String getLuckyMobMessage() {
        return config.getString("event_modifiers.lucky_mob.message",
                "&6✨ Sinh Vật May Mắn! &a+{points} điểm &7(x{multiplier}) &e- {mob}");
    }

    public boolean isLuckyMobAnnounceEnabled() {
        return config.getBoolean("event_modifiers.lucky_mob.announce_lucky_mob", true);
    }

    public String getLuckyMobAnnounceMessage() {
        return config.getString("event_modifiers.lucky_mob.announce_message",
                "&6✨ Sinh Vật May Mắn: &f{mob}&6! Tiêu diệt để có cơ hội x{multiplier} điểm!");
    }

    // ── Weather Buff ──────────────────────────────────────────
    public boolean isWeatherBuffEnabled() {
        return config.getBoolean("event_modifiers.weather_buff.enabled", true);
    }

    public double getRainBoostMultiplier() {
        return config.getDouble("event_modifiers.weather_buff.rain_boost.multiplier", 1.2);
    }

    public Set<Material> getRainBoostCrops() {
        return parseMaterialSet("event_modifiers.weather_buff.rain_boost.crops");
    }

    public String getRainBoostMessage() {
        return config.getString("event_modifiers.weather_buff.rain_boost.message",
                "&b☔ Buff Mưa! &a+{points} điểm &7(x{multiplier}) &e- {crop}");
    }

    public double getSunBoostMultiplier() {
        return config.getDouble("event_modifiers.weather_buff.sun_boost.multiplier", 1.2);
    }

    public Set<Material> getSunBoostCrops() {
        return parseMaterialSet("event_modifiers.weather_buff.sun_boost.crops");
    }

    public long getSunBoostTimeStart() {
        return config.getLong("event_modifiers.weather_buff.sun_boost.time_start", 5000L);
    }

    public long getSunBoostTimeEnd() {
        return config.getLong("event_modifiers.weather_buff.sun_boost.time_end", 7000L);
    }

    public String getSunBoostMessage() {
        return config.getString("event_modifiers.weather_buff.sun_boost.message",
                "&e☀ Buff Nắng! &a+{points} điểm &7(x{multiplier}) &e- {crop}");
    }

    // ── PlaceholderAPI fallback ───────────────────────────────
    public String getPapiTimeLeftFallback() {
        return config.getString("placeholder_fallback.time_left", "N/A");
    }

    public String getPapiTopNameFallback() {
        return config.getString("placeholder_fallback.top_name", "N/A");
    }

    public String getPapiTopPointsFallback() {
        return config.getString("placeholder_fallback.top_points", "0");
    }

    public String getPapiPlayerPointsFallback() {
        return config.getString("placeholder_fallback.player_points", "0");
    }

    // ── Helpers ───────────────────────────────────────────────
    private Set<Material> parseMaterialSet(String path) {
        Set<Material> result = new HashSet<>();
        List<String> list = config.getStringList(path);
        for (String entry : list) {
            try {
                result.add(Material.valueOf(entry.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(getConsoleMessage("invalid-material-warn").replace("{path}", path).replace("{entry}", entry));
            }
        }
        return result;
    }

    private Duration clampMin(Duration d) {
        if (d == null) return MIN_DURATION;
        return d.compareTo(MIN_DURATION) < 0 ? MIN_DURATION : d;
    }

    private Duration parseDuration(String duration) {
        if (duration == null) return Duration.ofMinutes(15);
        duration = duration.toLowerCase();
        try {
            if (duration.endsWith("s"))
                return Duration.ofSeconds(Long.parseLong(duration.substring(0, duration.length() - 1)));
            if (duration.endsWith("m"))
                return Duration.ofMinutes(Long.parseLong(duration.substring(0, duration.length() - 1)));
            if (duration.endsWith("h"))
                return Duration.ofHours(Long.parseLong(duration.substring(0, duration.length() - 1)));
            return Duration.ofMinutes(Long.parseLong(duration));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning(getConsoleMessage("invalid-duration-warn").replace("{value}", duration));
            return Duration.ofMinutes(15);
        }
    }
}
