package com.farmcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class MutationManager {

    private final FarmContest plugin;
    private File file;
    private FileConfiguration config;

    private final NamespacedKey keyMutationType;
    private final NamespacedKey keyMultiplier;
    private final NamespacedKey keyWeight;
    private final NamespacedKey keyOwner;
    private final NamespacedKey keyAutosell;
    private final NamespacedKey keyCropMaterial;

    private final Random random = new Random();

    public MutationManager(FarmContest plugin) {
        this.plugin = plugin;
        this.keyMutationType = new NamespacedKey(plugin, "mutation_type");
        this.keyMultiplier = new NamespacedKey(plugin, "mutation_multiplier");
        this.keyWeight = new NamespacedKey(plugin, "mutation_weight");
        this.keyOwner = new NamespacedKey(plugin, "mutation_owner");
        this.keyAutosell = new NamespacedKey(plugin, "autosell_enabled");
        this.keyCropMaterial = new NamespacedKey(plugin, "crop_material");

        loadConfig();
    }

    public boolean isAutosellEnabled(Player player) {
        return player.getPersistentDataContainer()
                .getOrDefault(keyAutosell, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    public void setAutosellEnabled(Player player, boolean enabled) {
        player.getPersistentDataContainer().set(keyAutosell, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0));
    }

    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        file = new File(plugin.getDataFolder(), "mutation.yml");
        if (!file.exists()) {
            plugin.saveResource("mutation.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }
    public void tryCreateMutation(Player player, Block block, Material cropMaterial) {
        if (!config.getBoolean("settings.enabled", true)) return;

        ConfigurationSection mutationsSec = config.getConfigurationSection("mutations");
        if (mutationsSec == null) return;

        double defaultChance = config.getDouble("settings.global_chance", 0.03);
        List<String> triggered = new ArrayList<>();

        for (String mutationKey : mutationsSec.getKeys(false)) {
            List<String> availableCrops = mutationsSec.getStringList(mutationKey + ".available_crops");
            if (!availableCrops.contains(cropMaterial.name())) continue;

            String condition = mutationsSec.getString(mutationKey + ".condition", "");
            if (!checkCondition(player, condition)) continue;

            double chance = mutationsSec.contains(mutationKey + ".chance")
                    ? mutationsSec.getDouble(mutationKey + ".chance")
                    : defaultChance;
            if (random.nextDouble() <= chance) triggered.add(mutationKey);
        }

        if (triggered.isEmpty()) return;

        if (!isMultiMutationEnabled(cropMaterial) && triggered.size() > 1) {
            triggered = List.of(pickHighestMultiplier(triggered));
        }

        ItemStack mutatedItem = createMutatedItemStack(player, cropMaterial, triggered);

        if (plugin.getConfigManager().isAutosellFeatureEnabled()
                && isAutosellEnabled(player) && plugin.getEconomy() != null) {
            double value = getItemValue(mutatedItem);
            plugin.getEconomy().depositPlayer(player, value);
            String msg = plugin.getConfigManager().getAutosellSoldMessage()
                    .replace("{crop}", formatCropName(cropMaterial))
                    .replace("{money}", String.format("%.2f", value));
            player.sendMessage(parseColor(msg));
            return;
        }

        Location loc = block.getLocation();
        if (loc.getWorld() != null) {
            loc.getWorld().dropItemNaturally(loc, mutatedItem);

            String mutationNames = triggered.stream()
                    .map(key -> config.getString("mutations." + key + ".display", key))
                    .reduce((a, b) -> a + " + " + b).orElse("");
            double multiplier = combinedMultiplier(triggered);

            String msg = plugin.getConfigManager().getMutationBroadcastMessage();
            msg = msg.replace("{mutation}", mutationNames)
                     .replace("{crop}", cropMaterial.name())
                     .replace("{multiplier}", String.valueOf(multiplier));

            player.sendMessage(parseColor(msg));
        }
    }

    public boolean isMultiMutationEnabled(Material cropMaterial) {
        String path = "settings.multi_mutation.crop_overrides." + cropMaterial.name();
        if (config.isSet(path)) return config.getBoolean(path);
        return config.getBoolean("settings.multi_mutation.enabled", false);
    }

    public double[] getWeightRange(Material cropMaterial) {
        String path = "settings.weight_by_crop." + cropMaterial.name();
        double min = config.getDouble(path + ".min", config.getDouble("settings.weight.min", 10.0));
        double max = config.getDouble(path + ".max", config.getDouble("settings.weight.max", 200.0));
        if (max < min) max = min;
        return new double[]{min, max};
    }

    private String pickHighestMultiplier(List<String> mutationKeys) {
        String best = mutationKeys.get(0);
        double bestMultiplier = config.getDouble("mutations." + best + ".multiplier", 1.0);
        for (String key : mutationKeys) {
            double multiplier = config.getDouble("mutations." + key + ".multiplier", 1.0);
            if (multiplier > bestMultiplier) {
                best = key;
                bestMultiplier = multiplier;
            }
        }
        return best;
    }

    private double combinedMultiplier(List<String> mutationKeys) {
        double product = 1.0;
        for (String key : mutationKeys) {
            product *= config.getDouble("mutations." + key + ".multiplier", 1.0);
        }
        return product;
    }

    public boolean checkCondition(Player player, String condition) {
        if (condition == null || condition.isEmpty()) return true;

        World world = player.getWorld();
        long time = world.getTime();

        if (condition.equalsIgnoreCase("rain")) {
            return world.hasStorm() && !world.isThundering();
        } else if (condition.equalsIgnoreCase("storm")) {
            return world.isThundering();
        } else if (condition.equalsIgnoreCase("midday")) {
            return time >= plugin.getConfigManager().getSunBoostTimeStart()
                    && time <= plugin.getConfigManager().getSunBoostTimeEnd()
                    && !world.hasStorm();
        } else if (condition.equalsIgnoreCase("midnight") || condition.equalsIgnoreCase("night")) {
            return time >= 13000 && time <= 23000;
        } else if (condition.equalsIgnoreCase("any")) {
            return true;
        } else if (condition.equalsIgnoreCase("snow_biome")) {
            Biome biome = player.getLocation().getBlock().getBiome();
            String name = biome.name();
            return name.contains("SNOW") || name.contains("FROZEN") || name.contains("ICE");
        } else if (condition.toLowerCase().startsWith("biome:")) {
            String targetBiomeStr = condition.substring(6).toUpperCase();
            try {
                Biome targetBiome = Biome.valueOf(targetBiomeStr);
                Biome playerBiome = player.getLocation().getBlock().getBiome();
                return playerBiome.equals(targetBiome);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }

    public ItemStack createMutatedItemStack(Player player, Material cropMaterial, String mutationKey) {
        return createMutatedItemStack(player, cropMaterial, List.of(mutationKey));
    }

    public ItemStack createMutatedItemStack(Player player, Material cropMaterial, List<String> mutationKeys) {
        ItemStack item = new ItemStack(cropMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String defaultName = plugin.getConfigManager().getMutationDefaultName();
        String defaultDisplay = plugin.getConfigManager().getMutationDefaultDisplay();

        List<String> names = new ArrayList<>();
        List<String> displays = new ArrayList<>();
        double multiplier = 1.0;
        for (String key : mutationKeys) {
            ConfigurationSection sec = config.getConfigurationSection("mutations." + key);
            names.add(sec != null ? sec.getString("name", defaultName) : defaultName);
            displays.add(sec != null ? sec.getString("display", defaultDisplay) : defaultDisplay);
            multiplier *= sec != null ? sec.getDouble("multiplier", 1.5) : 1.5;
        }
        String mutationName = String.join(" ", names);
        String mutationDisplay = String.join(" ", displays);

        double[] weightRange = getWeightRange(cropMaterial);
        double minWeight = weightRange[0];
        double maxWeight = weightRange[1];
        double weight = Math.round((minWeight + (maxWeight - minWeight) * random.nextDouble()) * 100.0) / 100.0;

        String ownerName = player.getName();
        String uuidStr = player.getUniqueId().toString();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyMutationType, PersistentDataType.STRING, String.join(",", mutationKeys));
        pdc.set(keyMultiplier, PersistentDataType.DOUBLE, multiplier);
        pdc.set(keyWeight, PersistentDataType.DOUBLE, weight);
        pdc.set(keyOwner, PersistentDataType.STRING, uuidStr);
        pdc.set(keyCropMaterial, PersistentDataType.STRING, cropMaterial.name());

        String nameFormat = plugin.getConfigManager().getMutationItemNameFormat();
        String cropDisplayName = formatCropName(cropMaterial);

        nameFormat = nameFormat.replace("{mutation_name}", mutationName)
                               .replace("{crop_display}", cropDisplayName);

        meta.displayName(parseColor(nameFormat));

        List<String> rawLore = plugin.getConfigManager().getMutationItemLore();
        List<Component> loreComponents = new ArrayList<>();
        for (String line : rawLore) {
            String processed = line.replace("{mutation_name}", mutationName)
                                   .replace("{mutation_display}", mutationDisplay)
                                   .replace("{crop_display}", cropDisplayName)
                                   .replace("{multiplier}", String.valueOf(multiplier))
                                   .replace("{weight}", String.valueOf(weight))
                                   .replace("{owner}", ownerName);
            loreComponents.add(parseColor(processed));
        }
        meta.lore(loreComponents);
        item.setItemMeta(meta);

        return item;
    }

    public boolean isMutatedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(keyMutationType, PersistentDataType.STRING) && pdc.has(keyWeight, PersistentDataType.DOUBLE);
    }

    public double getItemValue(ItemStack item) {
        if (!isMutatedItem(item)) return 0.0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        double weight = pdc.getOrDefault(keyWeight, PersistentDataType.DOUBLE, 10.0);
        double multiplier = pdc.getOrDefault(keyMultiplier, PersistentDataType.DOUBLE, 1.0);

        // Per-crop price override: shop.price_per_kg.<CROP> → fallback to shop.base_price_per_kg
        String cropName = pdc.getOrDefault(keyCropMaterial, PersistentDataType.STRING, "");
        double basePricePerKg;
        if (!cropName.isEmpty() && config.contains("shop.price_per_kg." + cropName)) {
            basePricePerKg = config.getDouble("shop.price_per_kg." + cropName, 50.0);
        } else {
            basePricePerKg = config.getDouble("shop.base_price_per_kg", 50.0);
        }

        return weight * multiplier * basePricePerKg;
    }

    private String formatCropName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private Component parseColor(String legacyAmpersandText) {
        String text = legacyAmpersandText == null ? "" : legacyAmpersandText;
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }
}