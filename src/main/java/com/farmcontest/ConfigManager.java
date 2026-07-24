package com.farmcontest;

import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.*;

public class ConfigManager {
    private final FarmContest plugin;
    private FileConfiguration config;

    public ConfigManager(FarmContest plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    // ── Contest core ──────────────────────────────────────────
    public boolean isContestEnabled() {
        return config.getBoolean("contest.enabled", true);
    }

    public Duration getContestInterval() {
        return parseDuration(config.getString("contest.interval", "3h"));
    }

    public Duration getContestDuration() {
        return parseDuration(config.getString("contest.duration", "15m"));
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

    // ── Console log messages ──────────────────────────────────
    public String getConsoleMessage(String key) {
        return config.getString("console-messages." + key, key);
    }

    // ── Mutation messages (dùng chung, tách khỏi mutation.yml) ─
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

    // ── Data ──────────────────────────────────────────────────
    public Duration getSaveInterval() {
        return parseDuration(config.getString("data.save-interval", "5m"));
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
